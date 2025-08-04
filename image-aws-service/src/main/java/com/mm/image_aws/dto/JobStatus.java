package com.mm.image_aws.dto;

public enum JobStatus {
    PENDING,    // Đang chờ xử lý
    PROCESSING, // Đang trong tiến trình
    COMPLETED,  // Hoàn thành
    FAILED      // Thất bại
}