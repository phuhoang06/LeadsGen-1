package com.mm.image_aws.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.mm.image_aws.dto.JobStatus;
import com.mm.image_aws.service.ImageProcessingService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ImageProcessorHandler implements RequestHandler<JobStatus, Object> {
    private static final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.mm.image_aws");
    private static final ImageProcessingService imageProcessingService = context.getBean(ImageProcessingService.class);

    @Override
    public Object handleRequest(JobStatus input, Context awsContext) {
        // Xử lý message từ SQS: download, upload S3, lưu metadata Oracle
        return imageProcessingService.processJob(input);
    }
}