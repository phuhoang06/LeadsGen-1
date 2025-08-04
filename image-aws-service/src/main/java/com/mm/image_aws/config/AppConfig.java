package com.mm.image_aws.config;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.util.concurrent.CompletableFuture;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // CẢI THIỆN: Tạo một Bean cho HttpAsyncClient với timeout settings
    @Bean
    public CloseableHttpAsyncClient httpAsyncClient() {
        // Cấu hình một connection pool mạnh mẽ
        final PoolingAsyncClientConnectionManager connectionManager = new PoolingAsyncClientConnectionManager();
        // Giới hạn tổng số kết nối đồng thời
        connectionManager.setMaxTotal(100);
        // Giới hạn số kết nối đến một host cụ thể (quan trọng!)
        connectionManager.setDefaultMaxPerRoute(20);

        CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setConnectionManager(connectionManager)
                // THÊM MỚI: Cấu hình timeout để fix DeadlineTimeoutException
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                        .setResponseTimeout(Timeout.ofSeconds(60))
                        .build())
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build();

        client.start();
        return client;
    }

    // Bean S3AsyncClient giữ nguyên
    @Bean
    public S3AsyncClient s3AsyncClient(AwsProperties awsProperties) {
        return S3AsyncClient.crtBuilder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(awsProperties.getAccessKey(), awsProperties.getSecretKey())
                        )
                )
                .region(Region.of(awsProperties.getRegion()))
                .build();
    }

    // Bean S3TransferManager giữ nguyên
    @Bean
    public S3TransferManager s3TransferManager(S3AsyncClient s3AsyncClient) {
        return S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();
    }
}