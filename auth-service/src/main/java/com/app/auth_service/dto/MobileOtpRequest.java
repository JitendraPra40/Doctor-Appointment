package com.app.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MobileOtpRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$", message = "Invalid mobile number")
    private String mobile;
}


