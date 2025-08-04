package com.mm.user.controller;

import com.mm.user.dto.BulkSignUpResponse;
import com.mm.user.dto.CsvImportRequest;
import com.mm.user.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class CsvImportController {

    private final CsvImportService csvImportService;

    @PostMapping("/users")
    public CompletableFuture<ResponseEntity<BulkSignUpResponse>> importUsers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(new BulkSignUpResponse("File is empty"))
            );
        }

        if (!file.getContentType().equals("text/csv") && 
            !file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(new BulkSignUpResponse("Only CSV files are allowed"))
            );
        }

        try {
            // Đọc toàn bộ file CSV thành String UTF-8
            String csvContent = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            // Truyền String sang service
            CompletableFuture<BulkSignUpResponse> future = csvImportService.processCsv(csvContent);
            long startTime = System.currentTimeMillis();
            
            return future.thenApply(response -> {
                long endTime = System.currentTimeMillis();
                double seconds = (endTime - startTime) / 1000.0;
                return ResponseEntity.ok(response);
            });
        } catch (Exception e) {
            log.error("Error processing CSV file", e);
            return CompletableFuture.completedFuture(
                ResponseEntity.internalServerError().body(
                    new BulkSignUpResponse("Error processing file: " + e.getMessage())
                )
            );
        }
    }
}
