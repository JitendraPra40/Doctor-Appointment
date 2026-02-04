package com.app.auth_service.service;

import com.app.auth_service.exception.OtpException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class RedisOtpService {

    private static final String OTP_PREFIX = "OTP:";
    private static final String ATTEMPT_PREFIX = "OTP_ATTEMPT:";

    private final StringRedisTemplate redis;

    public RedisOtpService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void saveOtp(String identifier, String otp) {
        redis.opsForValue()
                .set(OTP_PREFIX + identifier, otp, Duration.ofMinutes(5));

        // reset attempts on new OTP
//        redis.delete(ATTEMPT_PREFIX + email);
    }

    public void verifyOtp(String identifier, String otp) {

        String key = OTP_PREFIX + identifier;
        String storedOtp = redis.opsForValue().get(key);

        Long attempts;
        if (storedOtp == null) {
            attempts = redis.opsForValue().increment(ATTEMPT_PREFIX + identifier);
            throw new OtpException("OTP expired");
        }else{
            redis.delete(OTP_PREFIX + identifier);
        }

        // brute-force protection (max 5 attempts)
        attempts = redis.opsForValue().increment(ATTEMPT_PREFIX + identifier);

        if (attempts != null && attempts > 5) {
            redis.delete(key);
            throw new OtpException("Too many invalid attempts");
        }

        if (!storedOtp.equals(otp)) {
            throw new OtpException("Invalid OTP");
        }

        // ✅ OTP used successfully → delete it
        redis.delete(key);
        redis.delete(ATTEMPT_PREFIX + identifier);
    }
}



