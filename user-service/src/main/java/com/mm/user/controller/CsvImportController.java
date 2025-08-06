package com.mm.user.controller;

import com.mm.user.dto.BulkSignUpResponse;
import com.mm.user.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class CsvImportController {

    private final CsvImportService csvImportService;

    /**
     * Endpoint để tải lên và xử lý file CSV người dùng một cách bất đồng bộ.
     * Kết hợp ưu điểm của cả hai phiên bản:
     * 1. Controller trả về CompletableFuture để xử lý bất đồng bộ hoàn toàn.
     * 2. Phản hồi có cấu trúc với DTO (BulkSignUpResponse).
     * 3. Validation file đầy đủ.
     * 4. Xử lý file theo dòng (streaming) để tiết kiệm bộ nhớ (logic nằm trong CsvImportService).
     *
     * @param file File CSV được tải lên.
     * @return Một CompletableFuture chứa kết quả xử lý.
     */
    @PostMapping("/users")
    public CompletableFuture<ResponseEntity<BulkSignUpResponse>> importUsers(@RequestParam("file") MultipartFile file) {
        // Validation file đầu vào
        if (file.isEmpty()) {
            log.warn("Upload request received with an empty file.");
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(new BulkSignUpResponse("File is empty. Please select a CSV file to upload."))
            );
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        if (contentType == null || (!contentType.equals("text/csv") && (filename == null || !filename.toLowerCase().endsWith(".csv")))) {
            log.warn("Upload request received with an invalid file type: {}", contentType);
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(new BulkSignUpResponse("Invalid file type. Only CSV files are allowed."))
            );
        }

        try {
            log.info("Received request to import users from file: {}. Starting asynchronous processing.", filename);
            // Truyền trực tiếp đối tượng MultipartFile vào service để xử lý streaming
            // Service sẽ trả về một CompletableFuture<BulkSignUpResponse>
            return csvImportService.processCsvFile(file)
                    .thenApply(ResponseEntity::ok) // Khi future hoàn thành, gói kết quả vào ResponseEntity.ok()
                    .exceptionally(ex -> {
                        // Nếu có lỗi xảy ra trong quá trình xử lý bất đồng bộ
                        log.error("Error processing CSV file asynchronously", ex);
                        return ResponseEntity.internalServerError().body(
                            new BulkSignUpResponse("An unexpected error occurred: " + ex.getMessage())
                        );
                    });
        } catch (Exception e) {
            // Bắt các lỗi có thể xảy ra ngay lập tức (ví dụ: lỗi cấu hình)
            log.error("Error initiating CSV file processing", e);
            return CompletableFuture.completedFuture(
                ResponseEntity.internalServerError().body(
                    new BulkSignUpResponse("Error initiating file processing: " + e.getMessage())
                )
            );
        }
    }
}
