package com.mm.image_aws.repo;

import com.mm.image_aws.entity.ImageMetadata;
import com.mm.image_aws.entity.UploadJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ImageJobRepository {

    private final DynamoDbTable<UploadJob> jobTable;
    private final DynamoDbTable<ImageMetadata> metadataTable;

    public ImageJobRepository(DynamoDbEnhancedClient enhancedClient, @Value("${aws.dynamodb.table-name}") String tableName) {
        this.jobTable = enhancedClient.table(tableName, TableSchema.fromBean(UploadJob.class));
        this.metadataTable = enhancedClient.table(tableName, TableSchema.fromBean(ImageMetadata.class));
    }

    public void saveJob(UploadJob job) {
        jobTable.putItem(job);
    }
    
    public void saveMetadata(ImageMetadata metadata) {
        metadataTable.putItem(metadata);
    }

    public Optional<UploadJob> findJobById(String username, String jobId) {
        Key key = Key.builder()
                .partitionValue("USER#" + username)
                .sortValue("JOB#" + jobId)
                .build();
        return Optional.ofNullable(jobTable.getItem(key));
    }

    public List<UploadJob> findJobsByUsername(String username) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(k -> k.partitionValue("USER#" + username));
        return jobTable.query(queryConditional).items().stream().collect(Collectors.toList());
    }
    
    public List<ImageMetadata> findMetadataByUsername(String username) {
        // Đây là một truy vấn phức tạp hơn, cần có Global Secondary Index (GSI)
        // Để đơn giản, chúng ta sẽ lấy tất cả các job, sau đó lấy metadata cho từng job
        List<UploadJob> jobs = findJobsByUsername(username);
        return jobs.stream()
                .flatMap(job -> findMetadataByJobId(job.getJobId()).stream())
                .collect(Collectors.toList());
    }

    public List<ImageMetadata> findMetadataByJobId(String jobId) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(k -> k.partitionValue("JOB#" + jobId));
        return metadataTable.query(queryConditional).items().stream().collect(Collectors.toList());
    }

    public UploadJob updateJob(UploadJob job) {
        // UpdateItemEnhancedRequest cho phép cập nhật một item đã tồn tại
        UpdateItemEnhancedRequest<UploadJob> request = UpdateItemEnhancedRequest.builder(UploadJob.class)
                .item(job)
                .build();
        return jobTable.updateItem(request);
    }
}
