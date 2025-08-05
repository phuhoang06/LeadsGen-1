package com.mm.image_aws.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "upload_jobs")
@Data
public class UploadJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @Column(nullable = false)
    private String username;

    // === SỬA LỖI: Đổi tên trường theo quy ước camelCase ===
    @Column(name = "total_images", nullable = false)
    private int totalImages;

    @Column(name = "processed_images", nullable = false)
    private int processedImages;
    // =======================================================

    @Column(nullable = false)
    private String status; // e.g., PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ImageMetadata> imageMetadata;
}
