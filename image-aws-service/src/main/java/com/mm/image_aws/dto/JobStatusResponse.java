package com.mm.image_aws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mm.image_aws.entity.ImageMetadata;
import com.mm.image_aws.entity.UploadJob;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua các trường null khi trả về JSON
public class JobStatusResponse {
    private String jobId;
    private JobStatus status;
    private String progress;
    private List<String> cdnUrls;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Phương thức tĩnh để chuyển đổi từ UploadJob Entity sang DTO này.
     * @param job Entity từ database.
     * @return DTO để trả về cho người dùng.
     */
    public static JobStatusResponse fromJob(UploadJob job) {
        JobStatusResponse response = new JobStatusResponse();
        response.setJobId(job.getJobId() != null ? job.getJobId().toString() : null);
        response.setStatus(job.getStatus());
        response.setCreatedAt(job.getCreatedAt());
        response.setUpdatedAt(job.getUpdatedAt());

        response.setProgress(job.getProcessedUrls() + "/" + job.getTotalUrls());

        // Lấy danh sách các URL đã xử lý thành công
        if (job.getImageMetadataList() != null) {
            List<String> successfulUrls = job.getImageMetadataList().stream()
                    .filter(metadata -> metadata.getCdnUrl() != null && !metadata.getCdnUrl().isEmpty())
                    .map(ImageMetadata::getCdnUrl)
                    .collect(Collectors.toList());
            if (!successfulUrls.isEmpty()) {
                response.setCdnUrls(successfulUrls);
            }
        }

        if (job.getStatus() == JobStatus.FAILED) {
            response.setErrorMessage(job.getErrorMessage());
        }

        return response;
    }
}
