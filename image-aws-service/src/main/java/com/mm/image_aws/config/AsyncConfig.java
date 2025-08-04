package com.mm.image_aws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // === TỐI ƯU: Tăng số lượng threads để xử lý nhiều URLs cùng lúc ===
        executor.setCorePoolSize(20); // Tăng từ 12 lên 20
        executor.setMaxPoolSize(50);  // Tăng từ 12 lên 50
        executor.setQueueCapacity(500); // Tăng từ 200 lên 500
        executor.setThreadNamePrefix("UrlUpload-");
        executor.initialize();
        return executor;
    }
}