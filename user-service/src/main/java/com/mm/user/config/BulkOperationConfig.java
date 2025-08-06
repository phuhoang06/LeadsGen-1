package com.mm.user.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import lombok.Data;
import jakarta.validation.constraints.Min;


@Configuration
@ConfigurationProperties(prefix = "bulk.operation")
@Validated
@Data // Annotation của Lombok, tự động tạo getters, setters, toString, etc.
public class BulkOperationConfig {

    @Min(1)
    private int batchSize = 1000; // Kích thước lô mặc định

    @Min(1)
    private int corePoolSize = 4; // Số luồng xử lý chính

    @Min(1)
    private int maxPoolSize = 10; // Số luồng tối đa

    @Min(1)
    private int queueCapacity = 100; // Sức chứa của hàng đợi


}
