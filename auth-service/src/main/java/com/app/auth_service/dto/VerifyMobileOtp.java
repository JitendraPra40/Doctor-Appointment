package com.app.auth_service.dto;

import lombok.Data;


@Data
public class VerifyMobileOtp {
    private String mobile;
    private String otp;
}
