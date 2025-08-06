package com.mm.image_aws.service;

import com.mm.image_aws.config.AwsProperties;
import com.mm.image_aws.dto.DownloadedImage;
import com.mm.image_aws.exception.FileTooLargeException;
import com.mm.image_aws.service.transformer.UrlTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageDownloaderService {

    private final CloseableHttpAsyncClient httpAsyncClient;
    private final List<UrlTransformer> urlTransformers;
    private final AwsProperties awsProperties;

    /**
     * Tải dữ liệu ảnh từ một URL.
     * @param imageUrl URL của ảnh.
     * @return CompletableFuture chứa đối tượng DownloadedImage (bao gồm cả content và content type).
     */
    public CompletableFuture<DownloadedImage> downloadImage(String imageUrl) {
        CompletableFuture<DownloadedImage> future = new CompletableFuture<>();

        try {
            validateImageUrl(imageUrl);
            String directImageUrl = normalizeUrlForDirectDownload(imageUrl);
            final SimpleHttpRequest request = SimpleHttpRequest.create("GET", URI.create(directImageUrl));

            httpAsyncClient.execute(request, new FutureCallback<SimpleHttpResponse>() {
                @Override
                public void completed(SimpleHttpResponse response) {
                    try {
                        if (response.getCode() != 200) {
                            throw new RuntimeException("Server trả về mã lỗi: " + response.getCode());
                        }
                        byte[] contentBytes = response.getBodyBytes();
                        if (contentBytes == null || contentBytes.length == 0) {
                            throw new IllegalArgumentException("Nội dung trả về rỗng.");
                        }

                        // THÊM MỚI: Kiểm tra dung lượng file
                        validateFileSize(contentBytes.length, imageUrl);

                        // Lấy content type thực tế từ response
                        String contentType = Optional.ofNullable(response.getContentType())
                                .map(Object::toString)
                                .orElse("application/octet-stream");

                        // Trả về đối tượng DTO mới
                        future.complete(new DownloadedImage(contentBytes, contentType));

                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }

                @Override
                public void failed(Exception ex) {
                    future.completeExceptionally(ex);
                }

                @Override
                public void cancelled() {
                    future.cancel(true);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * @param fileSizeBytes Dung lượng file tính bằng bytes
     * @param imageUrl URL của ảnh (để log)
     */
    private void validateFileSize(long fileSizeBytes, String imageUrl) {
        // Sử dụng cấu hình từ properties, fallback về constant nếu không có
        long maxFileSize = awsProperties.getMaxFileSize();
        
        if (fileSizeBytes > maxFileSize) {
            double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
            double maxFileSizeMB = maxFileSize / (1024.0 * 1024.0);
            
            String errorMessage = String.format(
                "File quá lớn: %.2f MB (giới hạn: %.2f MB) - URL: %s", 
                fileSizeMB, maxFileSizeMB, imageUrl
            );
            
            log.warn(errorMessage);
            throw new FileTooLargeException(errorMessage, fileSizeBytes, maxFileSize);
        }
        
        // Log thông tin dung lượng file nếu debug
        if (log.isDebugEnabled()) {
            double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
            log.debug("File size: {:.2f} MB - URL: {}", fileSizeMB, imageUrl);
        }
    }

    private String normalizeUrlForDirectDownload(String url) {
        return urlTransformers.stream()
                .map(transformer -> transformer.transform(url))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(url);
    }

    private void validateImageUrl(String url) {
        if (url == null || !url.matches("^https?://.*")) {
            throw new IllegalArgumentException("URL không hợp lệ: " + url);
        }
    }
}