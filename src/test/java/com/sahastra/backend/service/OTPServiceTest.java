package com.sahastra.backend.service;

import com.sahastra.backend.domain.entity.OTP;
import com.sahastra.backend.domain.entity.User;
import com.sahastra.backend.domain.repository.OTPRepository;
import com.sahastra.backend.exception.BusinessException;
import com.sahastra.backend.security.util.OTPUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OTPServiceTest {

    @Mock
    private OTPRepository otpRepository;

    private OTPUtil otpUtil;
    private OTPService otpService;

    @BeforeEach
    void setUp() {
        otpUtil = new OTPUtil(new org.springframework.security.crypto.argon2.Argon2PasswordEncoder(16, 32, 1, 60, 10));
        otpService = new OTPService(otpRepository, otpUtil, 5, 2);
    }

    @Test
    void verifyEmailMarksOtpVerified() {
        String code = "123456";
        User user = User.builder().email("user@example.com").build();
        OTP otp = OTP.builder()
                .user(user)
                .email("user@example.com")
                .codeHash(otpUtil.hashOTPCode(code))
                .expiresAt(Instant.now().plusSeconds(60))
                .maxAttempts(2)
                .build();
        when(otpRepository.findByEmailAndExpiresAtAfter(eq("user@example.com"), any(Instant.class)))
                .thenReturn(Optional.of(otp));

        otpService.verifyEmail("user@example.com", code);

        verify(otpRepository).save(otp);
    }

    @Test
    void invalidCodeConsumesAttemptAndFails() {
        OTP otp = OTP.builder()
                .email("user@example.com")
                .codeHash(otpUtil.hashOTPCode("123456"))
                .expiresAt(Instant.now().plusSeconds(60))
                .maxAttempts(2)
                .build();
        when(otpRepository.findByEmailAndExpiresAtAfter(eq("user@example.com"), any(Instant.class)))
                .thenReturn(Optional.of(otp));

        assertThrows(BusinessException.class, () -> otpService.verifyEmail("user@example.com", "654321"));

        verify(otpRepository).save(otp);
    }
}
