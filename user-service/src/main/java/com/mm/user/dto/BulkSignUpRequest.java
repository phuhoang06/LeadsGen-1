package com.mm.user.dto;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class BulkSignUpRequest {
    @NotNull(message = "Users list cannot be null")
    @Size(min = 1, max = 1000, message = "Batch size must be between 1 and 1000")
    private List<@Valid SignUpRequest> users;
}
