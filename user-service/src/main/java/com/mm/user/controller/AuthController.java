package com.mm.user.controller;


import com.mm.user.dto.*;
import com.mm.user.entity.User;
import com.mm.user.repo.UserRepository;
import com.mm.user.security.JwtTokenProvider;
import com.mm.user.service.UserBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
// BỎ PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    // BỎ PasswordEncoder khỏi constructor
    private final JwtTokenProvider tokenProvider;
    private final UserBatchService userBatchService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            return ResponseEntity.ok(new AuthResponse(jwt));

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sai thông tin đăng nhập", "message", "Email/Username hoặc mật khẩu không chính xác."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody SignUpRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username đã tồn tại!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email đã được sử dụng!"));
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail());
        // SỬA ĐỔI: Lưu mật khẩu trực tiếp, không mã hóa
        user.setPassword(signUpRequest.getPassword());

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đăng ký người dùng thành công!"));
    }

    @PostMapping("/bulk-register")
    public ResponseEntity<?> registerMultipleUsers(@Valid @RequestBody BulkSignUpRequest bulkRequest) {
        try {
            if (bulkRequest.getUsers() == null || bulkRequest.getUsers().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Danh sách users không được để trống"));
            }

            if (bulkRequest.getUsers().size() > 1000) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Số lượng users tối đa mỗi lần gửi là 1000"
                ));
            }

            // SỬA ĐỔI: Truyền null cho PasswordEncoder vì chúng ta không mã hóa mật khẩu
            BulkSignUpResponse response = userBatchService.registerBatchUsers(
                    bulkRequest.getUsers(),
                    null // Không cần PasswordEncoder vì không mã hóa
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi đăng ký hàng loạt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Đã xảy ra lỗi khi xử lý yêu cầu đăng ký hàng loạt")
            );
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Token không được để trống"));
            }

            if (tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromJWT(token);
                return ResponseEntity.ok(Map.of("valid", true, "username", username));
            } else {
                return ResponseEntity.ok(Map.of("valid", false, "error", "Token không hợp lệ"));
            }
        } catch (Exception e) {
            logger.error("Lỗi khi validate token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("valid", false, "error", "Lỗi server khi validate token")
            );
        }
    }

    // ... các phương thức còn lại
}