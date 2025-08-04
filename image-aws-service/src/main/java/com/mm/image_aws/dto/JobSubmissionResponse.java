package com.mm.image_aws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobSubmissionResponse {
    private String jobId;
    private String statusUrl;
}