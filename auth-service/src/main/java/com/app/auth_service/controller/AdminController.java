package com.app.auth_service.controller;

import com.app.auth_service.dto.AccessTokenResponse;
import com.app.auth_service.dto.EmailOtpRequest;
import com.app.auth_service.dto.TokensDto;
import com.app.auth_service.dto.VerifyEmailOtp;
import com.app.auth_service.service.AdminService;
import com.app.auth_service.utility.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/admin")
public class AdminController {

    private final CookieUtil cookieUtil;
    private final AdminService adminService;

    public AdminController(CookieUtil cookieUtil, AdminService adminService) {
        this.cookieUtil = cookieUtil;
        this.adminService = adminService;
    }

    @PostMapping("/sendEmailOtp")
    public void sendEmailOtp(@RequestBody EmailOtpRequest emailOtpRequest) {
        System.out.println("sending otp..");
        adminService.sendEmailOtp(emailOtpRequest.getEmail());
    }

    @PostMapping("/otpEmailVerify")
    public ResponseEntity<AccessTokenResponse> verifyEmailOtp(@RequestBody VerifyEmailOtp verifyEmailOtp, HttpServletResponse response){
        TokensDto tokens = adminService.verifyEmailOtp(verifyEmailOtp.getEmail(), verifyEmailOtp.getOtp());
        cookieUtil.setRefreshToken(response, tokens.getRefreshToken());
        return ResponseEntity.ok(new AccessTokenResponse(tokens.getAccessToken()));
    }
}
