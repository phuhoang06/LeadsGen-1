package com.mm.image_aws.service;

import com.mm.image_aws.dto.DownloadedImage;
import com.mm.image_aws.exception.FileTooLargeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mm.image_aws.dto.JobStatus;

@Service
@Slf4j
public class ImageProcessingService {

    private final ImageDownloaderService downloaderService;
    private final S3StorageService s3StorageService;
    private final MetadataService metadataService;
    private final UploadJobService uploadJobService;

    public ImageProcessingService(ImageDownloaderService downloaderService,
                                  S3StorageService s3StorageService,
                                  MetadataService metadataService,
                                  @Lazy UploadJobService uploadJobService) { // <-- Thêm @Lazy ở đây
        this.downloaderService = downloaderService;
        this.s3StorageService = s3StorageService;
        this.metadataService = metadataService;
        this.uploadJobService = uploadJobService;
    }

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION_MAP = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/gif", ".gif")
    );

    @Async("taskExecutor")
    // Bỏ @Transactional ở đây để mỗi service con tự quản lý transaction của nó
    public void processImage(Long jobId, String imageUrl) {
        final byte[][] imageBytesHolder = {null};

        downloaderService.downloadImage(imageUrl)
                .thenCompose(downloadedImage -> {
                    imageBytesHolder[0] = downloadedImage.getContent();
                    String contentType = downloadedImage.getContentType();
                    String extension = CONTENT_TYPE_TO_EXTENSION_MAP.getOrDefault(contentType, ".jpg");
                    String fileName = UUID.randomUUID().toString() + extension;

                    return s3StorageService.upload(fileName, contentType, downloadedImage.getContent())
                            .thenApply(completedUpload -> {
                                String cdnUrl = s3StorageService.buildS3PublicUrl(fileName);
                                // Khi thành công, gọi metadata service với đầy đủ thông tin
                                metadataService.extractAndSaveMetadata(jobId, imageUrl, cdnUrl, downloadedImage.getContent(), null);
                                return cdnUrl;
                            });
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        // THÊM MỚI: Xử lý FileTooLargeException một cách riêng biệt
                        if (throwable.getCause() instanceof FileTooLargeException) {
                            FileTooLargeException fileTooLargeException = (FileTooLargeException) throwable.getCause();
                            String errorMessage = String.format(
                                "File quá lớn: %s (giới hạn: %s) - URL: %s", 
                                fileTooLargeException.getFileSizeMB(), 
                                fileTooLargeException.getMaxFileSizeMB(), 
                                imageUrl
                            );
                            
                            log.warn("Bỏ qua file quá lớn: {}", errorMessage);
                            
                            // Lưu metadata với thông tin lỗi file quá lớn
                            metadataService.extractAndSaveMetadata(jobId, imageUrl, null, null, errorMessage);
                            
                            // Cập nhật job status (vẫn tính là processed nhưng thất bại)
                            uploadJobService.updateJobAfterProcessing(jobId);
                                                 } else {
                             // Xử lý các lỗi khác như bình thường
                             log.error("Xử lý thất bại cho URL {}: {}", imageUrl, throwable.getMessage());
                             metadataService.extractAndSaveMetadata(jobId, imageUrl, null, null, throwable.getMessage());
                             uploadJobService.updateJobAfterProcessing(jobId);
                         }
                     } else {
                         log.info("Xử lý thành công URL: {}", imageUrl);
                         uploadJobService.updateJobAfterProcessing(jobId);
                     }
                });
    }

    @Async("taskExecutor")
    public void processMultipleImages(Long jobId, List<String> imageUrls) {
        log.info("Bắt đầu xử lý {} URLs cho job {}", imageUrls.size(), jobId);
        
        // Xử lý song song tất cả URLs
        imageUrls.parallelStream().forEach(imageUrl -> {
            try {
                processImage(jobId, imageUrl);
            } catch (Exception e) {
                                 log.error("Lỗi khi xử lý URL {}: {}", imageUrl, e.getMessage());
                 // Vẫn cập nhật job status để đảm bảo không bị treo
                 uploadJobService.updateJobAfterProcessing(jobId);
            }
        });
    }

    /**
     * Handler cho AWS Lambda: nhận JobStatus từ SQS, xử lý job tương ứng
     */
    public Object processJob(JobStatus jobStatus) {
        // TODO: Xử lý logic thực tế dựa trên jobStatus
        log.info("Received jobStatus from SQS: {}", jobStatus);
        // Có thể gọi processImage/processMultipleImages nếu cần
        return "OK";
    }
}
