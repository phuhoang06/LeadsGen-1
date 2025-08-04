package com.mm.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkSignUpResponse {
    private int totalUsers;
    private int successCount;
    private int errorCount;
    private List<UserResult> results;
    private String message; // Thêm trường message cho thông báo lỗi

    // Constructor cho trường hợp lỗi
    public BulkSignUpResponse(String message) {
        this.message = message;
        this.totalUsers = 0;
        this.successCount = 0;
        this.errorCount = 0;
        this.results = null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResult {
        private String username;
        private String email;
        private boolean success;
        private String message;
    }
}
