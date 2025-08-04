package com.mm.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
@EnableAsync
@EnableTransactionManagement
public class BulkOperationConfig {
    
    @Bean("bulkOperationExecutor")
    public Executor bulkOperationExecutor() {
        return new ThreadPoolExecutor(
            16, // Tăng core pool size từ 8 lên 16
            32, // Tăng max pool size từ 16 lên 32
            60L, // keep alive time
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000), // Tăng work queue từ 500 lên 1000
            new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
        );
    }
} 