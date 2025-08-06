package com.mm.user.service;

import com.mm.user.config.BulkOperationConfig;
import com.mm.user.dto.BulkSignUpResponse;
import com.mm.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class CsvImportService {

    private final UserBatchService userBatchService;
    private final BulkOperationConfig bulkOperationConfig;

    /**
     * Xử lý file CSV được tải lên một cách bất đồng bộ.
     * Phương thức này đọc file theo dòng để tiết kiệm bộ nhớ, chia thành các lô (batches),
     * và gửi chúng đến UserBatchService để xử lý song song.
     *
     * @param file Đối tượng MultipartFile chứa dữ liệu CSV.
     * @return Một CompletableFuture chứa đối tượng BulkSignUpResponse với kết quả tổng hợp.
     */
    @Async("taskExecutor") // Chạy toàn bộ phương thức này trên một luồng riêng
    public CompletableFuture<BulkSignUpResponse> processCsvFile(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        AtomicInteger totalRecords = new AtomicInteger(0);
        List<CompletableFuture<List<User>>> batchFutures = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            List<User> userBatch = new ArrayList<>();
            reader.readLine(); // Bỏ qua dòng header

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 3) {
                    totalRecords.incrementAndGet();
                    User user = new User(data[0].trim(), data[1].trim(), data[2].trim());
                    userBatch.add(user);
                }

                if (userBatch.size() == bulkOperationConfig.getBatchSize()) {
                    batchFutures.add(userBatchService.processBatch(new ArrayList<>(userBatch)));
                    userBatch.clear();
                }
            }

            if (!userBatch.isEmpty()) {
                batchFutures.add(userBatchService.processBatch(userBatch));
            }

            // Đợi tất cả các lô xử lý hoàn thành
            CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();

            long successfulCount = batchFutures.stream()
                    .mapToLong(future -> {
                        try {
                            return future.get().size();
                        } catch (Exception e) {
                            log.error("A batch failed during processing.", e);
                            return 0;
                        }
                    })
                    .sum();

            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            String message = String.format("Processed %d records in %.2f seconds. Successful: %d, Failed: %d.",
                    totalRecords.get(), duration, successfulCount, totalRecords.get() - successfulCount);
            log.info(message);
            return CompletableFuture.completedFuture(new BulkSignUpResponse(message));

        } catch (Exception e) {
            log.error("Failed to process CSV file.", e);
            // Trả về một future đã hoàn thành với lỗi
            return CompletableFuture.failedFuture(new RuntimeException("Failed to process CSV file: " + e.getMessage()));
        }
    }
}
