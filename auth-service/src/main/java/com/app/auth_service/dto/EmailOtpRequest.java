package com.app.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EmailOtpRequest {
    @NotBlank
    // FIX: Removed trailing '\n' from regex pattern — it made the pattern always fail validation
    // Original: "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$\n"
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "Invalid email")
    private String email;
}
