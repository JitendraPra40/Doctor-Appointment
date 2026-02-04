package com.app.auth_service.controller;

import com.app.auth_service.dto.*;
import com.app.auth_service.service.AuthService;
import com.app.auth_service.service.RefreshTokenService;
import com.app.auth_service.utility.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;
    public AuthController(AuthService authService, RefreshTokenService refreshTokenService, CookieUtil cookieUtil) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/sendMobileOtp")
    public void sendOtp(@RequestBody MobileOtpRequest mobileOtpRequest) {
        authService.sendMobileOtp(mobileOtpRequest.getMobile());
    }

    @PostMapping("/otpMobileVerify")
    public ResponseEntity<AccessTokenResponse> verifyMobileOtp(@RequestBody VerifyMobileOtp verifyMobileOtp, HttpServletResponse response){
        TokensDto tokens = authService.verifyMobileOtp(verifyMobileOtp.getMobile(), verifyMobileOtp.getOtp());
        cookieUtil.setRefreshToken(response, tokens.getRefreshToken());
        return ResponseEntity.ok(new AccessTokenResponse(tokens.getAccessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String oldRefreshToken = cookieUtil.getRefreshToken(request);
        TokensDto tokens = refreshTokenService.refresh(oldRefreshToken);
        cookieUtil.setRefreshToken(response, tokens.getRefreshToken());
        return ResponseEntity.ok(new AccessTokenResponse(tokens.getAccessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response, HttpServletRequest request) {
        String oldRefreshToken = cookieUtil.getRefreshToken(request);
        refreshTokenService.revokeRefreshToken(oldRefreshToken);
        cookieUtil.clearRefreshToken(response);
        return ResponseEntity.ok("Logged out");
    }

}

