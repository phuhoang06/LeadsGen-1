package com.mm.user.config;

import com.mm.user.controller.AuthController;
import com.mm.user.controller.CsvImportController;
import com.mm.user.dto.LoginRequest;
import com.mm.user.dto.SignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class FunctionRouterConfig {

    private final AuthController authController;
    private final CsvImportController csvImportController;

    @Bean
    public Function<LoginRequest, ResponseEntity<?>> login() {
        return authController::authenticateUser;
    }

    @Bean
    public Function<SignUpRequest, ResponseEntity<?>> register() {
        return authController::registerUser;
    }

    @Bean
    public Function<Mono<MultipartFile>, CompletableFuture<ResponseEntity<?>>> importUsers() {
        // Bọc lệnh gọi controller để xử lý Mono
        return mono -> mono.flatMap(file -> {
            try {
                return Mono.fromCompletionStage(csvImportController.importUsers(file));
            } catch (Exception e) {
                return Mono.<ResponseEntity<?>>error(e);
            }
        }).toFuture();
    }
}