package com.mm.image_aws.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mm.image_aws.dto.CdnUrlResponse;
import com.mm.image_aws.dto.JobStatusResponse;
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.entity.ImageMetadata;
import com.mm.image_aws.entity.UploadJob;
import com.mm.image_aws.exception.ResourceNotFoundException; // THAY ĐỔI: Import exception mới
import com.mm.image_aws.repo.ImageJobRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadJobService {

    private final ImageJobRepository imageJobRepository;
    private final SqsAsyncClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue.url}")
    private String queueUrl;

    public String createJob(UploadRequest uploadRequest, String username) {
        List<String> urls = uploadRequest.getUrls();
        if (urls == null || urls.isEmpty()) {
            throw new IllegalArgumentException("URL list cannot be empty");
        }

        String jobId = UUID.randomUUID().toString();
        UploadJob job = new UploadJob();
        job.setJobId(jobId);
        job.setUsername(username);
        job.setTotalImages(urls.size());
        job.setProcessedImages(0);
        job.setStatus("PENDING");
        job.setCreatedAt(Instant.now().toString());
        job.setUpdatedAt(Instant.now().toString());

        imageJobRepository.saveJob(job);
        log.info("Created job with ID: {} for user: {} with {} URLs", job.getJobId(), username, urls.size());

        for (String url : urls) {
            try {
                SqsMessagePayload payload = new SqsMessagePayload(job.getJobId(), username, url);
                String messageBody = objectMapper.writeValueAsString(payload);
                SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(messageBody)
                        .messageGroupId(job.getJobId())
                        .messageDeduplicationId(UUID.randomUUID().toString())
                        .build();

                sqsClient.sendMessage(sendMessageRequest).thenAccept(response -> {
                    log.info("Successfully sent message for URL: {} with MessageId: {}", url, response.messageId());
                }).exceptionally(ex -> {
                    log.error("Failed to send message for URL: {}", url, ex);
                    return null;
                });

            } catch (JsonProcessingException e) {
                log.error("Error serializing SQS message payload for job ID: {}", job.getJobId(), e);
            }
        }
        log.info("Queued {} messages for job ID: {}", urls.size(), job.getJobId());

        return job.getJobId();
    }

    public JobStatusResponse getJobStatus(String username, String jobId) {
        UploadJob job = imageJobRepository.findJobById(username, jobId)
                // THAY ĐỔI: Sử dụng ResourceNotFoundException
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        return new JobStatusResponse(
                Long.parseLong(job.getJobId().replaceAll("[^0-9]", "")),
                job.getStatus(),
                job.getTotalImages(),
                job.getProcessedImages(),
                LocalDateTime.ofInstant(Instant.parse(job.getCreatedAt()), ZoneOffset.UTC),
                LocalDateTime.ofInstant(Instant.parse(job.getUpdatedAt()), ZoneOffset.UTC)
        );
    }

    public List<JobStatusResponse> getUserJobs(String username) {
        List<UploadJob> jobs = imageJobRepository.findJobsByUsername(username);
        return jobs.stream()
                .map(job -> new JobStatusResponse(
                        Long.parseLong(job.getJobId().replaceAll("[^0-g]", "")),
                        job.getStatus(),
                        job.getTotalImages(),
                        job.getProcessedImages(),
                        LocalDateTime.ofInstant(Instant.parse(job.getCreatedAt()), ZoneOffset.UTC),
                        LocalDateTime.ofInstant(Instant.parse(job.getUpdatedAt()), ZoneOffset.UTC)
                ))
                .collect(Collectors.toList());
    }

    public List<String> getUserCdnUrls(String username) {
        List<ImageMetadata> metadataList = imageJobRepository.findMetadataByUsername(username);
        return metadataList.stream()
                .map(ImageMetadata::getCdnUrl)
                .collect(Collectors.toList());
    }

    public List<CdnUrlResponse> getUserDetailedCdnUrls(String username) {
        List<ImageMetadata> metadataList = imageJobRepository.findMetadataByUsername(username);
        return metadataList.stream()
                .map(meta -> new CdnUrlResponse(
                        Long.parseLong(meta.getJobId().replaceAll("[^0-9]", "")),
                        meta.getOriginalUrl(),
                        meta.getCdnUrl(),
                        LocalDateTime.ofInstant(Instant.parse(meta.getCreatedAt()), ZoneOffset.UTC)
                ))
                .collect(Collectors.toList());
    }

    public synchronized void updateJobAfterProcessing(String username, String jobId) {
        UploadJob job = imageJobRepository.findJobById(username, jobId)
                // THAY ĐỔI: Sử dụng ResourceNotFoundException
                .orElseThrow(() -> new ResourceNotFoundException("Cannot update job. Job not found with ID: " + jobId));

        job.setProcessedImages(job.getProcessedImages() + 1);
        job.setUpdatedAt(Instant.now().toString());

        if (job.getProcessedImages() < job.getTotalImages()) {
            job.setStatus("PROCESSING");
        } else {
            job.setStatus("COMPLETED");
            log.info("Job {} has completed processing all {} images.", jobId, job.getTotalImages());
        }

        imageJobRepository.updateJob(job);
        log.debug("Updated Job {}: {}/{} images processed.", jobId, job.getProcessedImages(), job.getTotalImages());
    }

    @Data
    @AllArgsConstructor
    private static class SqsMessagePayload {
        private String jobId;
        private String username;
        private String imageUrl;
    }
}
