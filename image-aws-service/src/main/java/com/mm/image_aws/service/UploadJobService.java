package com.mm.image_aws.service;

import com.mm.image_aws.dto.CdnUrlResponse;
import com.mm.image_aws.dto.JobStatus;
import com.mm.image_aws.dto.JobStatusResponse;
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.entity.ImageMetadata;
import com.mm.image_aws.entity.UploadJob;
import com.mm.image_aws.entity.User;
import com.mm.image_aws.repo.UploadJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadJobService {
    private final UploadJobRepository uploadJobRepository;
    private final ImageProcessingService imageProcessingService;

    public Long createJob(UploadRequest uploadRequest, String username) {
        // === TỐI ƯU: Xử lý nhiều URLs cùng lúc ===
        List<String> urls = uploadRequest.getUrls();
        if (urls == null || urls.isEmpty()) {
            throw new IllegalArgumentException("No URLs provided in upload request");
        }

        // Tạo job với thông tin URLs
        UploadJob job = new UploadJob();
        // Không lưu tất cả URLs trong một chuỗi dài nữa
        // job.setImageUrl(String.join("|", urls)); 
        job.setImageUrl(""); // Để trống vì không cần lưu tất cả URLs
        job.setUrlCount(urls.size()); // Lưu số lượng URLs
        job.setStatus(JobStatus.PENDING);
        job.setUsername(username);
        job.setTotalUrls(urls.size());
        job.setProcessedUrls(0);
        
        UploadJob savedJob = uploadJobRepository.save(job);
        log.info("Created job with ID: {} for user: {} with {} URLs", savedJob.getJobId(), username, urls.size());
        
        // Xử lý tất cả URLs cùng lúc
        imageProcessingService.processMultipleImages(savedJob.getJobId(), urls);
        return savedJob.getJobId();
    }

    public JobStatusResponse getJobStatus(String jobId) {
        Long numericJobId;
        try {
            numericJobId = Long.parseLong(jobId);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid job ID format: " + jobId);
        }
        UploadJob job = uploadJobRepository.findById(numericJobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        return JobStatusResponse.fromJob(job);
    }

    // === PHẦN SỬA LỖI: Thêm phương thức bị thiếu ===
    /**
     * Cập nhật trạng thái của một job sau khi quá trình xử lý ảnh hoàn tất.
     * Phương thức này được gọi bởi ImageProcessingService.
     * Nó chạy trong một transaction mới để đảm bảo tính nhất quán của dữ liệu.
     * @param jobId ID của job cần cập nhật.
     */
    @Transactional
    public void updateJobAfterProcessing(Long jobId) {
        UploadJob job = uploadJobRepository.findById(jobId)
                .orElseThrow(() -> {
                    log.error("Attempted to update a non-existent job with ID: {}", jobId);
                    return new RuntimeException("Job not found with id: " + jobId);
                });

        // Đánh dấu là đã xử lý xong 1 URL
        job.setProcessedUrls(job.getProcessedUrls() + 1);

        // Kiểm tra xem đã xử lý xong tất cả URLs chưa
        if (job.getProcessedUrls() >= job.getTotalUrls()) {
            List<ImageMetadata> metadataList = job.getImageMetadataList();
            
            if (metadataList != null && !metadataList.isEmpty()) {
                // Kiểm tra xem có ít nhất 1 ảnh thành công không
                boolean hasSuccess = metadataList.stream()
                        .anyMatch(metadata -> metadata.getCdnUrl() != null && !metadata.getCdnUrl().isEmpty());
                
                if (hasSuccess) {
                    job.setStatus(JobStatus.COMPLETED);
                    // Lấy URL đầu tiên thành công làm S3Url chính
                    String firstSuccessUrl = metadataList.stream()
                            .filter(metadata -> metadata.getCdnUrl() != null && !metadata.getCdnUrl().isEmpty())
                            .findFirst()
                            .map(ImageMetadata::getCdnUrl)
                            .orElse("");
                    job.setS3Url(firstSuccessUrl);
                    job.setErrorMessage(null);
                } else {
                    job.setStatus(JobStatus.FAILED);
                    job.setErrorMessage("Tất cả URLs đều xử lý thất bại");
                }
            } else {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Không tìm thấy metadata của ảnh đã xử lý.");
            }
        } else {
            // Vẫn đang xử lý, giữ nguyên status PENDING
            job.setStatus(JobStatus.PENDING);
        }

        uploadJobRepository.save(job);
        log.info("Updated job {} status to {} (processed: {}/{})", 
                jobId, job.getStatus(), job.getProcessedUrls(), job.getTotalUrls());
    }

    /**
     * Lấy tất cả các job của user hiện tại
     * @param username User cần lấy danh sách job
     * @return Danh sách các JobStatusResponse chứa thông tin job và URL CDN
     */
    public List<JobStatusResponse> getUserJobs(String username) {
        List<UploadJob> userJobs = uploadJobRepository.findByUsername(username);
        return userJobs.stream()
                .map(JobStatusResponse::fromJob)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách tất cả các URL CDN đã upload thành công của user
     * @param username User cần lấy danh sách URL CDN
     * @return Danh sách các URL CDN đã upload thành công
     */
    public List<String> getUserCdnUrls(String username) {
        List<UploadJob> userJobs = uploadJobRepository.findByUsername(username);
        return userJobs.stream()
                .filter(job -> job.getImageMetadataList() != null)
                .flatMap(job -> job.getImageMetadataList().stream())
                .filter(metadata -> metadata.getCdnUrl() != null && !metadata.getCdnUrl().isEmpty())
                .map(ImageMetadata::getCdnUrl)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách chi tiết tất cả các URL CDN đã upload thành công của user
     * @param username User cần lấy danh sách URL CDN chi tiết
     * @return Danh sách các CdnUrlResponse chứa thông tin chi tiết về URL CDN
     */
    public List<CdnUrlResponse> getUserDetailedCdnUrls(String username) {
        List<UploadJob> userJobs = uploadJobRepository.findByUsername(username);
        return userJobs.stream()
                .filter(job -> job.getImageMetadataList() != null)
                .flatMap(job -> job.getImageMetadataList().stream())
                .filter(metadata -> metadata.getCdnUrl() != null && !metadata.getCdnUrl().isEmpty())
                .map(CdnUrlResponse::fromImageMetadata)
                .collect(Collectors.toList());
    }
    
    /**
     * Handler cho AWS Lambda: nhận UploadRequest, tạo job, trả về JobStatusResponse
     */
    public JobStatusResponse handleUpload(UploadRequest uploadRequest) {
        // Có thể hardcode username hoặc lấy từ uploadRequest nếu cần
        String username = "lambda-user";
        Long jobId = createJob(uploadRequest, username);
        return getJobStatus(jobId.toString());
    }
}
