package com.mm.image_aws.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mm.image_aws.dto.CdnUrlResponse;
import com.mm.image_aws.dto.JobStatusResponse;
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.entity.ImageMetadata;
import com.mm.image_aws.entity.UploadJob;
import com.mm.image_aws.repo.ImageMetadataRepository;
import com.mm.image_aws.repo.UploadJobRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadJobService {

    private final UploadJobRepository uploadJobRepository;
    private final ImageMetadataRepository imageMetadataRepository;
    private final SqsAsyncClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue.url}")
    private String queueUrl;

    @Value("${aws.cdn.domain-name}")
    private String cdnDomain;

    public Long createJob(UploadRequest uploadRequest, String username) {
        List<String> urls = uploadRequest.getUrls();
        if (urls == null || urls.isEmpty()) {
            throw new IllegalArgumentException("URL list cannot be empty");
        }

        UploadJob job = new UploadJob();
        job.setUsername(username);
        job.setTotalImages(urls.size());
        job.setProcessedImages(0);
        job.setStatus("PENDING");
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        UploadJob savedJob = uploadJobRepository.save(job);
        log.info("Created job with ID: {} for user: {} with {} URLs", savedJob.getJobId(), username, urls.size());

        for (String url : urls) {
            try {
                SqsMessagePayload payload = new SqsMessagePayload(savedJob.getJobId(), url);
                String messageBody = objectMapper.writeValueAsString(payload);
                SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(messageBody)
                        .messageGroupId(savedJob.getJobId().toString())
                        .messageDeduplicationId(UUID.randomUUID().toString())
                        .build();

                sqsClient.sendMessage(sendMessageRequest).thenAccept(response -> {
                    log.info("Successfully sent message for URL: {} with MessageId: {}", url, response.messageId());
                }).exceptionally(ex -> {
                    log.error("Failed to send message for URL: {}", url, ex);
                    return null;
                });

            } catch (JsonProcessingException e) {
                log.error("Error serializing SQS message payload for job ID: {}", savedJob.getJobId(), e);
            }
        }
        log.info("Queued {} messages for job ID: {}", urls.size(), savedJob.getJobId());

        return savedJob.getJobId();
    }

    public JobStatusResponse getJobStatus(Long jobId) {
        UploadJob job = uploadJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with ID: " + jobId));

        return new JobStatusResponse(
                job.getJobId(),
                job.getStatus(),
                job.getTotalImages(),
                job.getProcessedImages(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    public List<JobStatusResponse> getUserJobs(String username) {
        List<UploadJob> jobs = uploadJobRepository.findByUsernameOrderByCreatedAtDesc(username);
        return jobs.stream()
                .map(job -> new JobStatusResponse(
                        job.getJobId(),
                        job.getStatus(),
                        job.getTotalImages(),
                        job.getProcessedImages(),
                        job.getCreatedAt(),
                        job.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    public List<String> getUserCdnUrls(String username) {
        List<ImageMetadata> metadataList = imageMetadataRepository.findByJobUsernameOrderByCreatedAtDesc(username);
        return metadataList.stream()
                .map(ImageMetadata::getCdnUrl)
                .collect(Collectors.toList());
    }

    public List<CdnUrlResponse> getUserDetailedCdnUrls(String username) {
        List<ImageMetadata> metadataList = imageMetadataRepository.findByJobUsernameOrderByCreatedAtDesc(username);
        return metadataList.stream()
                .map(meta -> new CdnUrlResponse(
                        meta.getJob().getJobId(),
                        meta.getOriginalUrl(),
                        meta.getCdnUrl(),
                        meta.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * [THÊM MỚI] Cập nhật trạng thái của Job sau khi một ảnh được xử lý.
     *
     * Phương thức này được đánh dấu là `synchronized` để đảm bảo an toàn luồng (thread-safe).
     * Khi nhiều ảnh của cùng một job được xử lý song song, phương thức này
     * sẽ ngăn chặn các vấn đề về dữ liệu (race conditions).
     *
     * @param jobId ID của job cần cập nhật.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void updateJobAfterProcessing(Long jobId) {
        // Sử dụng findById và orElseThrow để lấy Job, đảm bảo job tồn tại
        UploadJob job = uploadJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Job với ID: " + jobId));

        // Tăng số lượng ảnh đã xử lý
        job.setProcessedImages(job.getProcessedImages() + 1);
        job.setUpdatedAt(LocalDateTime.now());

        // Cập nhật trạng thái
        if (job.getProcessedImages() < job.getTotalImages()) {
            job.setStatus("PROCESSING");
        } else {
            job.setStatus("COMPLETED");
            log.info("Job {} đã hoàn thành xử lý tất cả {} ảnh.", jobId, job.getTotalImages());
        }

        // Lưu lại thay đổi vào cơ sở dữ liệu
        uploadJobRepository.save(job);
        log.debug("Đã cập nhật Job {}: {}/{} ảnh đã xử lý.", jobId, job.getProcessedImages(), job.getTotalImages());
    }

    @Data
    @AllArgsConstructor
    private static class SqsMessagePayload {
        private Long jobId;
        private String imageUrl;
    }
}