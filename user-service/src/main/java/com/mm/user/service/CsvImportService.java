package com.mm.user.service;

import com.mm.user.dto.BulkSignUpResponse;
import com.mm.user.dto.SignUpRequest;
import com.mm.user.entity.User;
import com.mm.user.repo.UserRepository;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final int BATCH_SIZE = 1000;

    @Async("taskExecutor")
    public CompletableFuture<BulkSignUpResponse> processCsv(String csvContent) {
        List<SignUpRequest> users = new ArrayList<>();
        try {
            // Log dòng header đầu tiên
            String[] lines = csvContent.split("\r?\n");
            if (lines.length > 0) {
                //log.info("[DEBUG] Header dòng đầu tiên trong file CSV: {}", lines[0]);
            }
            // Log các trường có @CsvBindByName trong SignUpRequest
            //log.info("[DEBUG] Các trường @CsvBindByName trong SignUpRequest:");
            //for (java.lang.reflect.Field field : com.mm.user.dto.SignUpRequest.class.getDeclaredFields()) {
            //    if (field.isAnnotationPresent(com.opencsv.bean.CsvBindByName.class)) {
            //        com.opencsv.bean.CsvBindByName ann = field.getAnnotation(com.opencsv.bean.CsvBindByName.class);
            //        String column = ann.column().isEmpty() ? field.getName() : ann.column();
            //        log.info("- {} (column: '{}')", field.getName(), column);
            //    }
            //}
            java.io.Reader stringReader = new java.io.StringReader(csvContent);
            CsvToBean<SignUpRequest> csvToBean;
            try {
                csvToBean = new CsvToBeanBuilder<SignUpRequest>(stringReader)
                        .withType(SignUpRequest.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withIgnoreQuotations(true)
                        .withSeparator(',')
                        .build();
            } catch (Exception e) {
                log.error("[CSV] Lỗi khi khởi tạo CsvToBeanBuilder: {}", e.getMessage(), e);
                return CompletableFuture.completedFuture(new BulkSignUpResponse(0, 0, 0, new ArrayList<>(), "Lỗi khi khởi tạo CsvToBeanBuilder: " + e.getMessage()));
            }
            try {
                users = csvToBean.parse();
                //log.info("Số user đọc được từ file CSV: {}", users.size());
                //for (int i = 0; i < Math.min(users.size(), 10); i++) {
                //    log.info("Parsed user {}: {}", i + 1, users.get(i));
                //}
                //if (users.size() > 10) {
                //    log.info("... ({} users total)", users.size());
                //}
            } catch (Exception e) {
                log.error("[CSV] Lỗi khi parse CSV: {}", e.getMessage(), e);
                return CompletableFuture.completedFuture(new BulkSignUpResponse(0, 0, 0, new ArrayList<>(), "Lỗi khi parse CSV: " + e.getMessage()));
            }
        } catch (Exception e) {
            log.error("[CSV] Lỗi không xác định khi đọc file CSV: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(new BulkSignUpResponse(0, 0, 0, new ArrayList<>(), "Lỗi không xác định khi đọc file CSV: " + e.getMessage()));
        }
        
        // Process in batches (song song)
        int total = users.size();
        int success = 0;
        int failed = 0;
        List<CompletableFuture<BulkSignUpResponse>> futures = new ArrayList<>();
        for (int i = 0; i < users.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, users.size());
            List<SignUpRequest> batch = users.subList(i, end);
            // Chạy song song các batch
            futures.add(CompletableFuture.supplyAsync(() -> processBatch(batch), java.util.concurrent.Executors.newCachedThreadPool()));
        }
        // Chờ tất cả batch xong
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        // Tổng hợp kết quả
        for (CompletableFuture<BulkSignUpResponse> future : futures) {
            try {
                BulkSignUpResponse response = future.get();
                success += response.getSuccessCount();
                failed += response.getErrorCount();
            } catch (Exception e) {
                log.error("Error in batch import: {}", e.getMessage(), e);
                failed += BATCH_SIZE; // Đếm tất cả batch này là fail nếu lỗi
            }
        }
        return CompletableFuture.completedFuture(
            new BulkSignUpResponse(total, success, failed, null, null)
        );
    }
    
    @Transactional
    public BulkSignUpResponse processBatch(List<SignUpRequest> requests) {
        // Tối ưu: Lấy toàn bộ username/email đã tồn tại chỉ 1 lần
        List<String> usernames = requests.stream().map(SignUpRequest::getUsername).collect(Collectors.toList());
        List<String> emails = requests.stream().map(SignUpRequest::getEmail).collect(Collectors.toList());
        List<User> existedByUsername = userRepository.findByUsernameIn(usernames);
        List<User> existedByEmail = userRepository.findByEmailIn(emails);
        Set<String> existedUsernameSet = existedByUsername.stream().map(User::getUsername).collect(Collectors.toSet());
        Set<String> existedEmailSet = existedByEmail.stream().map(User::getEmail).collect(Collectors.toSet());

        List<User> newUsers = requests.stream()
            .filter(req -> !existedUsernameSet.contains(req.getUsername()) && !existedEmailSet.contains(req.getEmail()))
            .map(req -> {
                User user = new User();
                user.setS_id(UUID.randomUUID().toString().replace("-", ""));
                user.setUsername(req.getUsername());
                user.setName(req.getName());
                user.setEmail(req.getEmail());
                user.setPassword(req.getPassword()); // Không mã hóa password nữa
                user.setState("active");
                return user;
            })
            .collect(Collectors.toList());
        
        if (!newUsers.isEmpty()) {
            userRepository.saveAll(newUsers);
        }
        
        int success = newUsers.size();
        int failed = requests.size() - success;
        
        return new BulkSignUpResponse(requests.size(), success, failed, null, null);
    }
}
