package com.mm.image_aws.service.transformer;

import org.springframework.stereotype.Component;
import java.util.Optional;

@Component // Đánh dấu là một Spring Bean
public class DropboxUrlTransformer implements UrlTransformer {

    @Override
    public Optional<String> transform(String url) {
        if (url.contains("dropbox.com")) {
            // Để truy cập trực tiếp nội dung file gốc, cần sử dụng tham số 'raw=1'.
            // 'dl=1' đôi khi trả về trang HTML xem trước thay vì hình ảnh.
            // Đoạn mã này sẽ thay thế domain và đảm bảo tham số là 'raw=1'.
            return Optional.of(
                    url.replace("www.dropbox.com", "dl.dropboxusercontent.com")
                            .replace("dl=0", "raw=1")
            );
        }
        return Optional.empty();
    }
}