package com.mm.image_aws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CdnUrlResponse {
    private Long jobId;
    private String originalUrl;
    private String cdnUrl;
    private LocalDateTime createdAt;
}
