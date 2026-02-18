package com.app.auth_service.service;

import com.app.auth_service.config.JwtTokenProvider;
import com.app.auth_service.dto.TokensDto;
import com.app.auth_service.entity.Admin;
import com.app.auth_service.entity.User;
import com.app.auth_service.exception.NotFoundException;
import com.app.auth_service.propertiesConfig.JwtProperties;
import com.app.auth_service.repository.AdminRepository;
import com.app.auth_service.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;


@Service
public class AdminService {

    private final JwtProperties jwtProperties;
    private final RedisOtpService redisOtpService;
    private final JwtTokenProvider jwt;
    private final RefreshTokenService refreshTokenService;
    private final EmailOtpService emailOtpService;

    private long accessExpiry;
    private long refreshExpiry;
    private final UserRepository userRepository;

    @PostConstruct
    void init() {
        this.accessExpiry = jwtProperties.getAccess().getExpiration();
        this.refreshExpiry = jwtProperties.getRefresh().getExpiration();
    }
    public AdminService(JwtProperties jwtProperties, RedisOtpService redisOtpService, JwtTokenProvider jwt, RefreshTokenService refreshTokenService, EmailOtpService emailOtpService,
                        UserRepository userRepository) {
        this.jwtProperties = jwtProperties;
        this.redisOtpService = redisOtpService;
        this.jwt = jwt;
        this.refreshTokenService = refreshTokenService;
        this.emailOtpService = emailOtpService;
        this.userRepository = userRepository;

    }

    public void sendEmailOtp(String email) {
        email = email.trim();
        String otp = String.valueOf( new SecureRandom().nextInt(900000) + 100000 );
        redisOtpService.saveOtp(email, otp);
        emailOtpService.sendOtp(email, otp);
    }

    public TokensDto verifyEmailOtp(String email, String otp) {
        email = email.trim();
        redisOtpService.verifyOtp(email, otp);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Not Allowed"));

        String access = jwt.generateAccessToken(user, accessExpiry);
        String refresh = refreshTokenService.createRefreshToken(user);
        return new TokensDto(access, refresh);
    }

}
