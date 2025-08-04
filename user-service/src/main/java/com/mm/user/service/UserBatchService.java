package com.mm.user.service;

import com.mm.user.dto.BulkSignUpResponse;
import com.mm.user.dto.SignUpRequest;
import com.mm.user.entity.User;
import com.mm.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBatchService {
    
    private final UserRepository userRepository;
    
    @Transactional(rollbackFor = Exception.class)
    public List<User> saveBatch(List<User> users) {
        if (users.isEmpty()) {
            return users;
        }
        
        try {
            // Tối ưu: Sử dụng saveAll với batch size lớn hơn
            List<User> savedUsers = userRepository.saveAll(users);
            log.debug("Saved batch of {} users", savedUsers.size());
            return savedUsers;
        } catch (Exception e) {
            log.error("Error saving batch of {} users", users.size(), e);
            throw e;
        }
    }
    
    public long countUsers() {
        return userRepository.count();
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public BulkSignUpResponse registerBatchUsers(List<SignUpRequest> userRequests, PasswordEncoder passwordEncoder) {
        List<BulkSignUpResponse.UserResult> results = userRequests.stream()
                .map(request -> {
                    String username = request.getUsername();
                    String email = request.getEmail();
                    
                    // Kiểm tra trùng lặp
                    if (userRepository.existsByUsername(username)) {
                        return new BulkSignUpResponse.UserResult(
                                username, email, false, "Username already exists");
                    }
                    if (userRepository.existsByEmail(email)) {
                        return new BulkSignUpResponse.UserResult(
                                username, email, false, "Email already exists");
                    }
                    
                    try {
                        // Tạo user mới
                        User user = new User();
                        user.setS_id(UUID.randomUUID().toString().replace("-", ""));
                        user.setUsername(username);
                        user.setName(request.getName());
                        user.setEmail(email);
                        // SỬA ĐỔI: Lưu mật khẩu trực tiếp, không mã hóa
                        user.setPassword(request.getPassword());
                        user.setState("active");
                        
                        userRepository.save(user);
                        return new BulkSignUpResponse.UserResult(
                                username, email, true, "User registered successfully");
                                
                    } catch (Exception e) {
                        log.error("Error registering user {}: {}", username, e.getMessage());
                        return new BulkSignUpResponse.UserResult(
                                username, email, false, "Error: " + e.getMessage());
                    }
                })
                .collect(Collectors.toList());
        
        // Đếm số lượng thành công/thất bại
        int successCount = (int) results.stream().filter(r -> r.isSuccess()).count();
        int errorCount = results.size() - successCount;
        
        return new BulkSignUpResponse(
                results.size(),
                successCount,
                errorCount,
                results,
                null
        );
    }
}