package com.mm.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BulkSignUpResponse {

    private String summaryMessage;
    private long totalRecords;
    private long successfulCount;
    private long failedCount;
    private List<String> failedRecordDetails; // Danh sách chi tiết các dòng bị lỗi

    public BulkSignUpResponse(String summaryMessage) {
        this.summaryMessage = summaryMessage;
    }
}
