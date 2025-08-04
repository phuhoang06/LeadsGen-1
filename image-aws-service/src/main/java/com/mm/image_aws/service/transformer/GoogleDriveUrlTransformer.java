package com.mm.image_aws.service.transformer;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component // Đánh dấu là một Spring Bean để có thể tự động inject
public class GoogleDriveUrlTransformer implements UrlTransformer {

    // === TỐI ƯU: Cải thiện pattern để nhận diện nhiều loại Google Drive URLs ===
    private static final Pattern GOOGLE_DRIVE_URL_PATTERN =
            Pattern.compile("drive\\.google\\.com/(?:file/d/|open\\?id=|uc\\?id=|drive/folders/|thumbnail\\?id=)([a-zA-Z0-9_-]{28,})");

    @Override
    public Optional<String> transform(String url) {
        Matcher matcher = GOOGLE_DRIVE_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            String fileId = matcher.group(1);
            
            // === TỐI ƯU: Sử dụng direct download URL để tăng tốc độ ===
            // Thử các loại direct URLs khác nhau để tối ưu tốc độ
            String[] directUrls = {
                "https://drive.google.com/uc?export=download&id=" + fileId,
                "https://drive.google.com/uc?export=view&id=" + fileId,
                "https://drive.google.com/thumbnail?id=" + fileId + "&sz=w4500"
            };
            
            // Trả về URL đầu tiên (có thể thêm logic để test và chọn URL tốt nhất)
            return Optional.of(directUrls[0]);
        }
        return Optional.empty();
    }
}