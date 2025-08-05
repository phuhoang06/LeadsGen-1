package com.mm.image_aws.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.mm.image_aws.ImageServiceApplication;
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.service.UploadJobService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Collections;
import java.util.Map;

public class UploadTriggerHandler implements RequestHandler<UploadRequest, Map<String, Long>> {

    private static final ApplicationContext applicationContext;
    private static final UploadJobService uploadJobService;

    static {
        try {
            // Tái sử dụng cùng một phương pháp khởi tạo Spring Context
            applicationContext = new AnnotationConfigApplicationContext(ImageServiceApplication.class);
            uploadJobService = applicationContext.getBean(UploadJobService.class);
        } catch (Exception e) {
            System.err.println("FATAL: Could not initialize Spring Context for UploadTriggerHandler.");
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Context", e);
        }
    }

    /**
     * Xử lý yêu cầu tạo job upload.
     * @param input Đối tượng UploadRequest chứa danh sách URLs.
     * @param awsContext Context của AWS Lambda.
     * @return Một Map chứa jobId đã được tạo.
     */
    @Override
    public Map<String, Long> handleRequest(UploadRequest input, Context awsContext) {
        LambdaLogger logger = awsContext.getLogger();
        logger.log("UploadTriggerHandler invoked with " + (input.getUrls() != null ? input.getUrls().size() : 0) + " URLs.");

        // Vì handler này không có thông tin xác thực người dùng (JWT),
        // chúng ta sẽ dùng một username mặc định.
        // Trong thực tế, bạn có thể cần truyền thông tin người dùng vào trong 'input'.
        final String triggerUsername = "lambda-trigger-user";

        // [SỬA LỖI] Gọi đúng phương thức `createJob` thay vì `handleUpload` không tồn tại.
        Long jobId = uploadJobService.createJob(input, triggerUsername);

        logger.log("Successfully created Job with ID: " + jobId + " for user: " + triggerUsername);

        // Trả về kết quả theo định dạng mà API Controller đang dùng.
        return Collections.singletonMap("jobId", jobId);
    }
}