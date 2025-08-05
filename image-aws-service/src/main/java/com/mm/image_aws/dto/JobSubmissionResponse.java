package com.mm.image_aws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Đối tượng truyền dữ liệu (DTO) cho phản hồi khi một job được tạo thành công.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobSubmissionResponse {
    private String jobId;
}
