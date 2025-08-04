package com.mm.image_aws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CdnUrlResponse {
    private String jobId;
    private String originalUrl;
    private String cdnUrl;
    private Integer width;
    private Integer height;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private JobStatus status;

    public static CdnUrlResponse fromImageMetadata(com.mm.image_aws.entity.ImageMetadata metadata) {
        CdnUrlResponse response = new CdnUrlResponse();
        response.setJobId(metadata.getUploadJob().getJobId().toString());
        response.setOriginalUrl(metadata.getOriginalUrl());
        response.setCdnUrl(metadata.getCdnUrl());
        response.setWidth(metadata.getWidth());
        response.setHeight(metadata.getHeight());
        response.setFileSize(metadata.getFileSize());
        response.setUploadedAt(metadata.getUploadJob().getCreatedAt());
        response.setStatus(metadata.getUploadJob().getStatus());
        return response;
    }
} 