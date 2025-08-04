package com.mm.image_aws.controller;

import com.mm.image_aws.dto.CdnUrlResponse;
import com.mm.image_aws.dto.JobStatusResponse;
import com.mm.image_aws.dto.JobSubmissionResponse;
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.service.UploadJobService;
import com.mm.image_aws.service.RateLimitingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import com.mm.image_aws.dto.JobStatus;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UploadController {
    private final UploadJobService uploadJobService; // Uncomment để sử dụng service thật
    private final RateLimitingService rateLimitingService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@Valid @RequestBody UploadRequest uploadRequest, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        
        // === THÊM RATE LIMITING ===
        String userId = user.getUsername();
        if (!rateLimitingService.tryAcquire(userId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Rate limit exceeded",
                            "message", "Bạn chỉ có thể gửi 1 request upload trong 1 giây. Vui lòng thử lại sau.",
                            "retryAfter", "1 second"
                    ));
        }
        // === KẾT THÚC RATE LIMITING ===

        Long jobId = uploadJobService.createJob(uploadRequest, user.getUsername()); // Uncomment để sử dụng service thật

        // === PHẦN SỬA LỖI LOGIC ===
        // Tạo URL đầy đủ để kiểm tra trạng thái job thay vì chỉ trả về "PENDING"
        String statusUrl = "/api/jobs/" + jobId;
        return ResponseEntity.ok(new JobSubmissionResponse(String.valueOf(jobId), statusUrl));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        JobStatusResponse status = uploadJobService.getJobStatus(jobId); // Uncomment để sử dụng service thật
        return ResponseEntity.ok(status);
    }

    @GetMapping("/jobs")
    public ResponseEntity<?> getUserJobs(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        List<JobStatusResponse> userJobs = uploadJobService.getUserJobs(user.getUsername()); // Uncomment để sử dụng service thật
        return ResponseEntity.ok(userJobs);
    }

    @GetMapping("/cdn-urls")
    public ResponseEntity<?> getCdnUrls(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        List<String> cdnUrls = uploadJobService.getUserCdnUrls(user.getUsername()); // Uncomment để sử dụng service thật
        return ResponseEntity.ok(cdnUrls);
    }

    @GetMapping("/cdn-urls/detailed")
    public ResponseEntity<?> getDetailedCdnUrls(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        List<CdnUrlResponse> detailedUrls = uploadJobService.getUserDetailedCdnUrls(user.getUsername()); // Uncomment để sử dụng service thật
        return ResponseEntity.ok(detailedUrls);
    }

    @GetMapping("/rate-limit/status")
    public ResponseEntity<?> getRateLimitStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        String userId = user.getUsername();
        Long remainingTime = rateLimitingService.getRemainingTime(userId);
        
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "remainingTimeSeconds", remainingTime != null ? remainingTime : 0,
                "rateLimitInfo", Map.of(
                        "maxRequests", 1,
                        "timeWindowSeconds", 1,
                        "description", "1 request per second for upload endpoint"
                )
        ));
    }
}
