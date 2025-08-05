package com.mm.image_aws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
// === SỬA LỖI: Bổ sung các annotation cần thiết ===
@NoArgsConstructor
@AllArgsConstructor
// ===============================================
public class JobStatusResponse {
    private Long jobId;
    private String status;
    private int totalImages;
    private int processedImages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
