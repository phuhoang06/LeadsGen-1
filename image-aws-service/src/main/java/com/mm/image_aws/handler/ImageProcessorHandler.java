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

    // Khởi tạo Spring Context một lần duy nhất.
    // Điều này rất quan trọng để tối ưu hiệu năng cho các lần gọi Lambda "warm".
    private static final ApplicationContext applicationContext;
    private static final ImageProcessingService imageProcessingService;
    private static final ObjectMapper objectMapper;

    static {
        try {
            // Khởi tạo Spring Context bằng cách trỏ đến lớp Application chính của bạn.
            // Điều này đảm bảo tất cả các @Bean, @Service, @Repository đều được nạp.
            applicationContext = new AnnotationConfigApplicationContext(ImageServiceApplication.class);
            // Lấy các bean cần thiết từ context
            imageProcessingService = applicationContext.getBean(ImageProcessingService.class);
            objectMapper = applicationContext.getBean(ObjectMapper.class);
        } catch (Exception e) {
            // Nếu có lỗi nghiêm trọng trong quá trình khởi tạo, log và throw exception
            // để AWS Lambda biết rằng môi trường thực thi này đã bị lỗi.
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
                // Deserialization message từ SQS thành đối tượng Payload
                SqsMessagePayload payload = objectMapper.readValue(messageBody, SqsMessagePayload.class);

                if (payload.getJobId() == null || payload.getImageUrl() == null) {
                    logger.log("ERROR: Invalid message payload. JobId or ImageUrl is null.");
                    continue; // Bỏ qua message không hợp lệ
                }

                // Gọi service để xử lý ảnh.
                // Service này đã được inject đầy đủ dependencies nhờ Spring Context.
                imageProcessingService.processImage(payload.getJobId(), payload.getImageUrl());

            } catch (JsonProcessingException e) {
                logger.log("ERROR: Failed to parse SQS message body: " + messageBody + ". Error: " + e.getMessage());
            } catch (Exception e) {
                // Bắt các lỗi khác có thể xảy ra trong quá trình xử lý
                logger.log("ERROR: An unexpected error occurred while processing message: " + messageBody + ". Error: " + e.getMessage());
                // Cân nhắc: Có thể đẩy message này vào một Dead-Letter Queue (DLQ) để phân tích sau
            }
        }
        return null;
    }

    /**
     * Lớp DTO (Data Transfer Object) để ánh xạ nội dung JSON từ message trong SQS.
     * Cần phải khớp với cấu trúc được tạo ra trong `UploadJobService`.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SqsMessagePayload {
        private Long jobId;
        private String imageUrl;
    }
}