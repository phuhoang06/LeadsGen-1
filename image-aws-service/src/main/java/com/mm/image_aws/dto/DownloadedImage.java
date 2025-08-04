package com.mm.image_aws.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Một đối tượng truyền dữ liệu (DTO) đơn giản
 * để chứa cả nội dung (byte array) và kiểu file (content type) của ảnh đã tải về.
 */
@Getter
@AllArgsConstructor
public class DownloadedImage {
    private final byte[] content;
    private final String contentType;
}