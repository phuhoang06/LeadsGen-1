package com.mm.image_aws.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "IMAGE_METADATA")
public class ImageMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "image_metadata_seq")
    @SequenceGenerator(name = "image_metadata_seq", sequenceName = "IMAGE_METADATA_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    // --- THÊM MỐI QUAN HỆ MANY-TO-ONE ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UPLOAD_JOB_ID") // Tên cột khóa ngoại trong bảng IMAGE_METADATA
    private UploadJob uploadJob;
    // ------------------------------------

    @Lob
    @Column(name = "ORIGINAL_URL", nullable = false)
    private String originalUrl;

    @Lob
    @Column(name = "CDN_URL")
    private String cdnUrl;

    @Column(name = "WIDTH")
    private Integer width;

    @Column(name = "HEIGHT")
    private Integer height;

    @Column(name = "DPI")
    private Integer dpi;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;
}
