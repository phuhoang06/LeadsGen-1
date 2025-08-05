package com.mm.image_aws.entity;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Data
public class ImageMetadata {

    private String pk; // Partition Key: JOB#{jobId}
    private String sk; // Sort Key: IMAGE#{s3Key}

    private String jobId;
    private String username; // Thêm username để truy vấn dễ hơn
    private String originalUrl;
    private String cdnUrl;
    private String s3Key;
    private String format;
    private Integer width;
    private Integer height;
    private Long fileSize; // in bytes
    private Integer dpi;
    private String errorMessage;
    private String createdAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return "JOB#" + this.jobId;
    }
    public void setPk(String pk) {
        // Setter này cần thiết cho DynamoDB enhanced client
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return "IMAGE#" + this.s3Key;
    }
    public void setSk(String sk) {
        // Setter này cần thiết cho DynamoDB enhanced client
    }
}
