package com.mm.image_aws.controller;

import com.mm.image_aws.dto.CdnUrlResponse;
import com.mm.image_aws.dto.JobStatusResponse;
import com.mm.image_aws.dto.JobSubmissionResponse; // THÊM MỚI
import com.mm.image_aws.dto.UploadRequest;
import com.mm.image_aws.service.UploadJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UploadController {

    private final UploadJobService uploadJobService;

    @PostMapping("/upload")
    public ResponseEntity<JobSubmissionResponse> submitJob(@RequestBody UploadRequest uploadRequest, @AuthenticationPrincipal UserDetails userDetails) {
        String jobId = uploadJobService.createJob(uploadRequest, userDetails.getUsername());
        // SỬ DỤNG DTO MỚI ĐỂ PHẢN HỒI
        return ResponseEntity.ok(new JobSubmissionResponse(jobId));
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId, @AuthenticationPrincipal UserDetails userDetails) {
        JobStatusResponse status = uploadJobService.getJobStatus(userDetails.getUsername(), jobId);
        return ResponseEntity.ok(status);
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

