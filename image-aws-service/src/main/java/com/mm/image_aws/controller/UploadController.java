package com.mm.image_aws.controller;

import com.mm.image_aws.dto.CdnUrlResponse;
import com.mm.image_aws.dto.JobStatusResponse;
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.service.UploadJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UploadController {

    private final UploadJobService uploadJobService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Long>> submitJob(@RequestBody UploadRequest uploadRequest, @AuthenticationPrincipal UserDetails userDetails) {
        Long jobId = uploadJobService.createJob(uploadRequest, userDetails.getUsername());
        return ResponseEntity.ok(Collections.singletonMap("jobId", jobId));
    }

    @GetMapping("/status/{jobId}")
    // === SỬA LỖI: Đổi kiểu dữ liệu của jobId từ String thành Long ===
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(uploadJobService.getJobStatus(jobId));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobStatusResponse>> getUserJobs(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(uploadJobService.getUserJobs(userDetails.getUsername()));
    }

    @GetMapping("/cdn-urls")
    public ResponseEntity<List<String>> getUserCdnUrls(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(uploadJobService.getUserCdnUrls(userDetails.getUsername()));
    }

    @GetMapping("/cdn-urls/detailed")
    public ResponseEntity<List<CdnUrlResponse>> getUserDetailedCdnUrls(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(uploadJobService.getUserDetailedCdnUrls(userDetails.getUsername()));
    }
}
