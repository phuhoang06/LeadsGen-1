package com.mm.image_aws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Lớp này là điểm vào (entry point) mà AWS Lambda sẽ gọi.
 * Nó sử dụng AWS Serverless Java Container để dịch các sự kiện từ API Gateway
 * thành các yêu cầu HTTP mà Spring Boot có thể hiểu.
 */
public class StreamLambdaHandler implements RequestStreamHandler {
    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            // Khởi tạo ứng dụng Spring Boot của bạn một lần duy nhất
            // Điều này rất quan trọng để tận dụng hiệu suất của Lambda SnapStart
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(ImageServiceApplication.class);
        } catch (Exception e) {
            // Nếu có lỗi trong quá trình khởi tạo Spring, log lỗi và ném ra ngoại lệ
            // để Lambda biết rằng invocation đã thất bại.
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        // Ủy quyền xử lý request cho handler đã được khởi tạo
        handler.proxyStream(inputStream, outputStream, context);
    }
}
