package com.app.auth_service.dto;

import lombok.Data;

@Data
public class VerifyEmailOtp {

    private String email;
    private String otp;
}
