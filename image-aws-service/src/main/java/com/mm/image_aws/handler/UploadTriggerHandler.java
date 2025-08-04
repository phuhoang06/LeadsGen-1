package com.mm.image_aws.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.service.UploadJobService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class UploadTriggerHandler implements RequestHandler<UploadRequest, Object> {
    private static final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.mm.image_aws");
    private static final UploadJobService uploadJobService = context.getBean(UploadJobService.class);

    @Override
    public Object handleRequest(UploadRequest input, Context awsContext) {
        // Xử lý upload, tạo job, gửi SQS
        return uploadJobService.handleUpload(input);
    }
}