package com.mm.user.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

@Data
public class CsvImportRequest {
    @NotNull(message = "CSV file is required")
    private MultipartFile file;
}
