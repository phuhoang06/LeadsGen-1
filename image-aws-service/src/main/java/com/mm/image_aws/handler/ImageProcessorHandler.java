package com.mm.image_aws.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mm.image_aws.ImageServiceApplication;
import com.mm.image_aws.service.ImageProcessingService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ImageProcessorHandler implements RequestHandler<SQSEvent, Void> {

    private static final ApplicationContext applicationContext;
    private static final ImageProcessingService imageProcessingService;
    private static final ObjectMapper objectMapper;

    static {
        try {
            applicationContext = new AnnotationConfigApplicationContext(ImageServiceApplication.class);
            imageProcessingService = applicationContext.getBean(ImageProcessingService.class);
            objectMapper = applicationContext.getBean(ObjectMapper.class);
        } catch (Exception e) {
            System.err.println("FATAL: Could not initialize Spring Context for ImageProcessorHandler.");
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Context", e);
        }
    }

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Received " + sqsEvent.getRecords().size() + " SQS messages.");

        for (SQSEvent.SQSMessage msg : sqsEvent.getRecords()) {
            String messageBody = msg.getBody();
            logger.log("Processing message body: " + messageBody);

            try {
                SqsMessagePayload payload = objectMapper.readValue(messageBody, SqsMessagePayload.class);

                if (payload.getJobId() == null || payload.getImageUrl() == null || payload.getUsername() == null) {
                    logger.log("ERROR: Invalid message payload. JobId, Username, or ImageUrl is null.");
                    continue;
                }

                // Gọi service để xử lý ảnh với đầy đủ thông tin
                imageProcessingService.processImage(payload.getJobId(), payload.getUsername(), payload.getImageUrl());

            } catch (JsonProcessingException e) {
                logger.log("ERROR: Failed to parse SQS message body: " + messageBody + ". Error: " + e.getMessage());
            } catch (Exception e) {
                logger.log("ERROR: An unexpected error occurred while processing message: " + messageBody + ". Error: " + e.getMessage());
            }
        }
        return null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SqsMessagePayload {
        private String jobId;
        private String username;
        private String imageUrl;
    }
}