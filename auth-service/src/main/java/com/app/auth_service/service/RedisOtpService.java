package com.app.auth_service.service;

import com.app.auth_service.exception.OtpException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class RedisOtpService {

    private static final String OTP_PREFIX = "OTP:";
    private static final String ATTEMPT_PREFIX = "OTP_ATTEMPT:";
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redis;

    public RedisOtpService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void saveOtp(String identifier, String otp) {
        redis.opsForValue()
                .set(OTP_PREFIX + identifier, otp, Duration.ofMinutes(5));
        // Reset attempt counter on new OTP send
        redis.delete(ATTEMPT_PREFIX + identifier);
    }

    /**
     * FIX: Original logic had two critical bugs:
     * 1. The OTP was deleted BEFORE verification — meaning even wrong OTPs would consume the token.
     * 2. Attempt counter was incremented AFTER deleting a valid OTP, with the guard checked too late.
     *
     * Correct order: Check attempts → verify OTP → delete OTP + reset attempts on success.
     */
    public void verifyOtp(String identifier, String otp) {

        String otpKey = OTP_PREFIX + identifier;
        String attemptKey = ATTEMPT_PREFIX + identifier;

        // 1. Check if OTP exists (not expired)
        String storedOtp = redis.opsForValue().get(otpKey);
        if (storedOtp == null) {
            throw new OtpException("OTP expired or not found");
        }

        // 2. Brute-force protection — increment attempts BEFORE verifying
        Long attempts = redis.opsForValue().increment(attemptKey);
        // Set expiry on attempt key (align with OTP TTL)
        if (attempts != null && attempts == 1) {
            redis.expire(attemptKey, Duration.ofMinutes(5));
        }

        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redis.delete(otpKey); // Invalidate OTP after too many attempts
            throw new OtpException("Too many invalid attempts. Please request a new OTP.");
        }

        // 3. Verify OTP value
        if (!storedOtp.equals(otp)) {
            throw new OtpException("Invalid OTP");
        }

        // 4. OTP is valid — clean up
        redis.delete(otpKey);
        redis.delete(attemptKey);
    }
}
