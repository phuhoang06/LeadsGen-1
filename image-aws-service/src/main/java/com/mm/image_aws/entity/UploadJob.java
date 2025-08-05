package com.mm.image_aws.entity;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Data
public class UploadJob {

    private String pk; // Partition Key: USER#{username}
    private String sk; // Sort Key: JOB#{jobId}

    private String jobId;
    private String username;
    private int totalImages;
    private int processedImages;
    private String status;
    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return "USER#" + this.username;
    }
    public void setPk(String pk) {
        // Setter này cần thiết cho DynamoDB enhanced client, nhưng chúng ta sẽ không gọi nó trực tiếp
    }


    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return "JOB#" + this.jobId;
    }
    public void setSk(String sk) {
        // Setter này cần thiết cho DynamoDB enhanced client
    }
}
