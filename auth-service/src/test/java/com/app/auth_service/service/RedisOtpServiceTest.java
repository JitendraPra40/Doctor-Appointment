package com.app.auth_service.service;

import com.app.auth_service.exception.OtpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisOtpService Tests")
class RedisOtpServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private RedisOtpService service;

    private static final String EMAIL       = "user@example.com";
    private static final String VALID_OTP   = "123456";
    private static final String OTP_KEY     = "OTP:" + EMAIL;
    private static final String ATTEMPT_KEY = "OTP_ATTEMPT:" + EMAIL;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // saveOtp()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("saveOtp()")
    class SaveOtpTests {

        @Test
        @DisplayName("should store OTP in Redis with 5-minute TTL")
        void saveOtp_storesWithTtl() {
            service.saveOtp(EMAIL, VALID_OTP);

            verify(valueOps).set(eq(OTP_KEY), eq(VALID_OTP), eq(Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("should delete attempt counter when new OTP is saved (reset brute force)")
        void saveOtp_deletesAttemptCounter() {
            service.saveOtp(EMAIL, VALID_OTP);

            verify(redis).delete(ATTEMPT_KEY);
        }

        @Test
        @DisplayName("should use OTP: prefix for storage key")
        void saveOtp_usesCorrectKeyPrefix() {
            service.saveOtp("another@test.com", "654321");

            verify(valueOps).set(eq("OTP:another@test.com"), eq("654321"), any(Duration.class));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // verifyOtp()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("verifyOtp()")
    class VerifyOtpTests {

        @Test
        @DisplayName("should succeed and clean up Redis keys on valid OTP")
        void verifyOtp_valid_cleansUpKeys() {
            when(valueOps.get(OTP_KEY)).thenReturn(VALID_OTP);
            when(valueOps.increment(ATTEMPT_KEY)).thenReturn(1L);

            service.verifyOtp(EMAIL, VALID_OTP);

            verify(redis).delete(OTP_KEY);
            verify(redis).delete(ATTEMPT_KEY);
        }

        @Test
        @DisplayName("should throw OtpException when OTP is expired (key not in Redis)")
        void verifyOtp_expired_throwsOtpException() {
            when(valueOps.get(OTP_KEY)).thenReturn(null);

            OtpException ex = catchThrowableOfType(
                    () -> service.verifyOtp(EMAIL, VALID_OTP),
                    OtpException.class
            );

            assertThat(ex.getMessage()).contains("expired");
        }

        @Test
        @DisplayName("should throw OtpException with 'Invalid OTP' when OTP is wrong")
        void verifyOtp_wrongOtp_throwsInvalidOtp() {
            when(valueOps.get(OTP_KEY)).thenReturn(VALID_OTP);
            when(valueOps.increment(ATTEMPT_KEY)).thenReturn(1L);

            OtpException ex = catchThrowableOfType(
                    () -> service.verifyOtp(EMAIL, "999999"),
                    OtpException.class
            );

            assertThat(ex.getMessage()).contains("Invalid OTP");
        }

        @Test
        @DisplayName("should set TTL on attempt counter on first attempt")
        void verifyOtp_firstAttempt_setsTtlOnAttemptKey() {
            when(valueOps.get(OTP_KEY)).thenReturn(VALID_OTP);
            when(valueOps.increment(ATTEMPT_KEY)).thenReturn(1L);

            service.verifyOtp(EMAIL, VALID_OTP);

            verify(redis).expire(eq(ATTEMPT_KEY), eq(Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("should NOT set TTL on attempt counter after first attempt")
        void verifyOtp_secondAttempt_doesNotResetTtl() {
            when(valueOps.get(OTP_KEY)).thenReturn(VALID_OTP);
            when(valueOps.increment(ATTEMPT_KEY)).thenReturn(2L);  // 2nd attempt

            service.verifyOtp(EMAIL, VALID_OTP);

            verify(redis, never()).expire(any(), any());
        }

        @Test
        @DisplayName("should throw OtpException and delete OTP after exceeding max attempts")
        void verifyOtp_exceededMaxAttempts_throwsAndDeletesOtp() {
            when(valueOps.get(OTP_KEY)).thenReturn(VALID_OTP);
            when(valueOps.increment(ATTEMPT_KEY)).thenReturn(6L);  // > MAX_ATTEMPTS (5)

            OtpException ex = catchThrowableOfType(
                    () -> service.verifyOtp(EMAIL, VALID_OTP),
                    OtpException.class
            );

            assertThat(ex.getMessage()).contains("Too many");
            verify(redis).delete(OTP_KEY);  // OTP invalidated
        }

        @Test
        @DisplayName("should increment attempt counter before verifying OTP value")
        void verifyOtp_incrementsBeforeVerify() {
            when(valueOps.get(OTP_KEY)).thenReturn(VALID_OTP);
            when(valueOps.increment(ATTEMPT_KEY)).thenReturn(1L);

            // Even wrong OTP: attempt counter incremented
            try { service.verifyOtp(EMAIL, "wrong"); } catch (OtpException ignored) {}

            verify(valueOps).increment(ATTEMPT_KEY);
        }

        @Test
        @DisplayName("should NOT delete OTP keys on wrong OTP attempt (only on success)")
        void verifyOtp_wrongOtp_doesNotDeleteOtpKey() {
            when(valueOps.get(OTP_KEY)).thenReturn(VALID_OTP);
            when(valueOps.increment(ATTEMPT_KEY)).thenReturn(1L);

            try { service.verifyOtp(EMAIL, "wrong"); } catch (OtpException ignored) {}

            verify(redis, never()).delete(OTP_KEY);
        }
    }
}
