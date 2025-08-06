package com.mm.user.service;

import com.mm.user.entity.User;
import com.mm.user.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class UserBatchService {

    private static final Logger logger = LoggerFactory.getLogger(UserBatchService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserBatchService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CompletableFuture<List<User>> processBatch(List<User> userBatch) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Processing a batch of {} users in thread: {}", userBatch.size(), Thread.currentThread().getName());

            List<User> processedUsers = userBatch.stream()
                    .map(user -> {
                        // Mã hóa mật khẩu trước khi lưu
                        user.setPassword(passwordEncoder.encode(user.getPassword()));
                        // Gán quyền mặc định
                        user.setRoles(Set.of("ROLE_USER"));
                        return userRepository.save(user);
                    })
                    .collect(Collectors.toList());

            logger.info("Successfully processed and saved a batch of {} users.", processedUsers.size());
            return processedUsers;
        });
    }
}
