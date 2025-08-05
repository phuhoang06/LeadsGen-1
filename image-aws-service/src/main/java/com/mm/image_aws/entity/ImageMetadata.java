package com.mm.image_aws.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_metadata")
@Data
public class ImageMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metadata_id")
    private Long metadataId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @ToString.Exclude
    private UploadJob job;

    @Column(name = "original_url", length = 2048)
    private String originalUrl;

    @Column(name = "cdn_url", length = 2048)
    private String cdnUrl;

    @Column(name = "s3_key")
    private String s3Key;

    private String format; // e.g., JPEG, PNG

    // [SỬA LỖI] Thay đổi từ 'int' thành 'Integer' để cho phép giá trị null
    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_size")
    private long fileSize; // in bytes

    // [THÊM MỚI] Bổ sung trường DPI
    @Column(name = "dpi")
    private Integer dpi;

    // [THÊM MỚI] Bổ sung trường để lưu thông báo lỗi
    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // Gán giá trị mặc định
}