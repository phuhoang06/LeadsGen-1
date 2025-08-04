package com.mm.image_aws.service;

import com.mm.image_aws.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    private final S3TransferManager transferManager;
    private final AwsProperties config;

    public CompletableFuture<CompletedUpload> upload(String fileName, String contentType, byte[] content) {
        UploadRequest uploadRequest = UploadRequest.builder()
                .putObjectRequest(req -> req.bucket(config.getBucket()).key(fileName).contentType(contentType))
                .requestBody(AsyncRequestBody.fromBytes(content))
                .build();

        log.info("Bắt đầu upload file {} lên S3.", fileName);
        Upload upload = transferManager.upload(uploadRequest);
        return upload.completionFuture();
    }

    public String buildS3PublicUrl(String fileName) {
        if (config.getCdnDomain() != null && !config.getCdnDomain().isEmpty()) {
            String cdnDomain = config.getCdnDomain();
            String prefix = cdnDomain.startsWith("http") ? "" : "https://";
            return prefix + cdnDomain + "/" + fileName;
        }
        return "https://" + config.getBucket() + ".s3." + config.getRegion() + ".amazonaws.com/" + fileName;
    }
}
