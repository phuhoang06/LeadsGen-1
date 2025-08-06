package com.mm.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;

@Configuration
@EnableAsync
public class AsyncConfig {

    private final BulkOperationConfig bulkOperationConfig;

    public AsyncConfig(BulkOperationConfig bulkOperationConfig) {
        this.bulkOperationConfig = bulkOperationConfig;
    }

    /**
     * Định nghĩa một bean ExecutorService để xử lý các tác vụ bất đồng bộ.
     * Bean này sẽ được Spring sử dụng cho các phương thức được đánh dấu @Async.
     *
     * @return một instance của ExecutorService đã được cấu hình.
     */
    @Bean(name = "taskExecutor")
    public ExecutorService taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(bulkOperationConfig.getCorePoolSize());
        executor.setMaxPoolSize(bulkOperationConfig.getMaxPoolSize());
        executor.setQueueCapacity(bulkOperationConfig.getQueueCapacity());
        executor.setThreadNamePrefix("UserBatch-");
        executor.initialize();
        // Trả về đối tượng ExecutorService thực thi bên trong
        return executor.getThreadPoolExecutor();
    }
}
