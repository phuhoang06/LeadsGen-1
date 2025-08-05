package com.mm.image_aws.service;

import com.mm.image_aws.exception.FileTooLargeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ImageProcessingService {

    private final ImageDownloaderService downloaderService;
    private final S3StorageService s3StorageService;
    private final MetadataService metadataService;
    private final UploadJobService uploadJobService;

    // Sử dụng @Lazy để tránh lỗi circular dependency có thể xảy ra
    public ImageProcessingService(ImageDownloaderService downloaderService,
                                  S3StorageService s3StorageService,
                                  MetadataService metadataService,
                                  @Lazy UploadJobService uploadJobService) {
        this.downloaderService = downloaderService;
        this.s3StorageService = s3StorageService;
        this.metadataService = metadataService;
        this.uploadJobService = uploadJobService;
    }

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION_MAP = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/webp", ".webp")
    );

    @Async("taskExecutor")
    public void processImage(String jobId, String username, String imageUrl) {
        downloaderService.downloadImage(imageUrl)
                .thenCompose(downloadedImage -> {
                    String contentType = downloadedImage.getContentType();
                    String extension = CONTENT_TYPE_TO_EXTENSION_MAP.getOrDefault(contentType, ".jpg");
                    String s3Key = UUID.randomUUID().toString() + extension;

                    return s3StorageService.upload(s3Key, contentType, downloadedImage.getContent())
                            .thenAccept(completedUpload -> {
                                String cdnUrl = s3StorageService.buildS3PublicUrl(s3Key);
                                // Khi thành công, gọi metadata service với đầy đủ thông tin
                                metadataService.extractAndSaveMetadata(jobId, username, imageUrl, cdnUrl, s3Key, downloadedImage.getContent(), null);
                            });
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        String errorMessage;
                        if (throwable.getCause() instanceof FileTooLargeException) {
                            FileTooLargeException ex = (FileTooLargeException) throwable.getCause();
                            errorMessage = String.format("File quá lớn: %s (giới hạn: %s)", ex.getFileSizeMB(), ex.getMaxFileSizeMB());
                            log.warn("{} - URL: {}", errorMessage, imageUrl);
                        } else {
                            errorMessage = throwable.getMessage();
                            log.error("Xử lý thất bại cho URL {}: {}", imageUrl, errorMessage);
                        }
                        // Dù lỗi gì cũng lưu lại metadata với thông báo lỗi
                        metadataService.extractAndSaveMetadata(jobId, username, imageUrl, null, null, null, errorMessage);
                    } else {
                        log.info("Xử lý thành công URL: {}", imageUrl);
                    }
                    // Luôn cập nhật trạng thái job sau khi xử lý xong (thành công hoặc thất bại)
                    uploadJobService.updateJobAfterProcessing(username, jobId);
                });
    }
}
