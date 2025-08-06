package com.mm.image_aws.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
@Data
public class AwsProperties {
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
    private String cdnDomain;
    

    private long maxFileSize = 5 * 1024 * 1024; // 5MB
    
    // Getter cho maxFileSize
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    // Setter cho maxFileSize
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    public static final String MAX_FILE_SIZE_MB = "5MB";
}