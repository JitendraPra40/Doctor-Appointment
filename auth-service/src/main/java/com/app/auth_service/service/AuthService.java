package com.app.auth_service.service;

import com.app.auth_service.propertiesConfig.JwtProperties;
import com.app.auth_service.dto.TokensDto;
import com.app.auth_service.entity.User;
import com.app.auth_service.repository.UserRepository;
import com.app.auth_service.roles.Role;
import com.app.auth_service.config.JwtTokenProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;


@Service
@Transactional
public class AuthService {

    private final JwtProperties jwtProperties;
    private final EmailOtpService emailOtpService;
    private final UserRepository userRepo;
    private final RefreshTokenService refreshTokenService;
    private final RedisOtpService redisOtpService;
    private final JwtTokenProvider jwt;
    private final TwilioSmsService twilioSmsService;

    private long accessExpiry;
    private long refreshExpiry;

    @PostConstruct
    void init() {
        this.accessExpiry = jwtProperties.getAccess().getExpiration();
        this.refreshExpiry = jwtProperties.getRefresh().getExpiration();
    }

    public AuthService(JwtProperties jwtProperties, EmailOtpService emailOtpService, UserRepository userRepo,
                       RefreshTokenService refreshTokenService,
                       RedisOtpService redisOtpService,
                       JwtTokenProvider jwt, TwilioSmsService twilioSmsService) {
        this.jwtProperties = jwtProperties;
        this.emailOtpService = emailOtpService;
        this.userRepo = userRepo;
        this.refreshTokenService = refreshTokenService;
        this.redisOtpService = redisOtpService;
        this.jwt = jwt;
        this.twilioSmsService = twilioSmsService;
    }

    public void sendMobileOtp(String mobile) {
        mobile = mobile.trim();
        String normalizedMobile = "+91" + mobile;
        String otp = String.valueOf( new SecureRandom().nextInt(900000) + 100000 );
        redisOtpService.saveOtp(normalizedMobile, otp);
        twilioSmsService.sendOtp(normalizedMobile, otp);
    }

    public TokensDto verifyMobileOtp(String mobile, String otp) {
        mobile = mobile.trim();
        String normalizedMobile = "+91" + mobile;
        redisOtpService.verifyOtp(normalizedMobile, otp);
        User user = userRepo.findByMobile(normalizedMobile)
                .orElseGet(() -> {
                    User u = new User();
                    u.setMobile(normalizedMobile);
                    u.setRole(Role.PATIENT);
                    return userRepo.save(u);
                });
        String access = jwt.generateAccessToken(user, accessExpiry);
        String refresh = refreshTokenService.createRefreshToken(user);
        return new TokensDto(access, refresh);
    }




    // User login and Registration through email
//    public void sendEmailOtp(String email) {
//        email = email.trim();
//        String otp = String.valueOf( new SecureRandom().nextInt(900000) + 100000 );
//        redisOtpService.saveOtp(email, otp);
//        emailOtpService.sendOtp(email, otp);
//    }
//
//    public TokensDto verifyEmailOtp(String email, String otp){
//        String e = email.trim();
//        redisOtpService.verifyOtp(email, otp);
//        User user = userRepo.findByEmail(email)
//                .orElseGet(() -> {
//                    User u = new User();
//                    u.setEmail(email);
//                    u.setRole(Role.PATIENT);
//                    return userRepo.save(u);
//                });
//        String access = jwt.generateAccessToken(user, accessExpiry);
//        String refresh = refreshTokenService.createRefreshToken(user);
//        return new TokensDto(access, refresh);
//    }


    // This approch is for if you want a proper user registration with all fields and provide login with mobile or email
//    public String register(String username, String mobile, String email){
//        User user = new User();
//        user.setUsername(username);
//        user.setMobile(mobile);
//        user.setEmail(email);
//        userRepo.save(user);
//        return "Register Successfully";
//    }
//    public void sendOtp(String identifier){
//        identifier = identifier.trim();
//        if (identifier.contains("@")){
//            sendEmailOtp(identifier);
//        }else{
//            sendMobileOtp(identifier);
//        }
//    }
//    public TokensDto verifyOtp(String identifier, String otp) {
//
//        identifier = identifier.trim();
//        redisOtpService.verifyOtp(identifier, otp);
//
//        User user;
//
//        if (identifier.contains("@")) {
//            user = userRepo.findByEmail(identifier)
//                    .orElseThrow(() -> new NotFoundException("User not allowed"));
//        } else {
//            user = userRepo.findByMobile(identifier)
//                    .orElseThrow(() -> new NotFoundException("User not allowed"));
//        }
//
//        String access = jwt.generateAccessToken(user, accessExpiry);
//        String refresh = refreshTokenService.createRefreshToken(user);
//
//        return new TokensDto(access, refresh);
//    }

}

