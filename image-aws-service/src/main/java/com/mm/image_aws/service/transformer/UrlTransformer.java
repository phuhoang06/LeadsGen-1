package com.mm.image_aws.service.transformer;

import java.util.Optional;

/**
 * Interface định nghĩa một "chiến lược" để biến đổi một URL chia sẻ
 * thành một URL có thể tải trực tiếp.
 */
public interface UrlTransformer {

    /**
     * Cố gắng biến đổi URL đã cho.
     * @param url URL đầu vào.
     * @return một Optional chứa URL đã biến đổi nếu thành công,
     * hoặc Optional.empty() nếu URL này không được hỗ trợ bởi chiến lược này.
     */
    Optional<String> transform(String url);
}