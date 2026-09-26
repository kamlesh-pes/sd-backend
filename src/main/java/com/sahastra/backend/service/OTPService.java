package com.sahastra.backend.service;

import com.sahastra.backend.domain.entity.OTP;
import com.sahastra.backend.domain.entity.User;
import com.sahastra.backend.domain.repository.OTPRepository;
import com.sahastra.backend.exception.BusinessException;
import com.sahastra.backend.exception.ResourceNotFoundException;
import com.sahastra.backend.security.util.OTPUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OTPService {

    private static final Pattern SIX_DIGITS = Pattern.compile("\\d{6}");
    private final OTPRepository otpRepository;
    private final OTPUtil otpUtil;
    private final long expiryMinutes;
    private final int maxAttempts;
    private final SystemSettingService systemSettingService;

    @Autowired
    public OTPService(OTPRepository otpRepository, OTPUtil otpUtil,
                      SystemSettingService systemSettingService) {
        this.otpRepository = otpRepository;
        this.otpUtil = otpUtil;
        this.expiryMinutes = 5;
        this.maxAttempts = 5;
        this.systemSettingService = systemSettingService;
    }

    public OTPService(OTPRepository otpRepository, OTPUtil otpUtil, long expiryMinutes, int maxAttempts) {
        this.otpRepository = otpRepository;
        this.otpUtil = otpUtil;
        this.expiryMinutes = expiryMinutes;
        this.maxAttempts = maxAttempts;
        this.systemSettingService = null;
    }

    @Transactional
    public void generateForEmail(User user, String email) {
        String normalizedEmail = email.trim().toLowerCase();
        String code = otpUtil.generateOTPCode();
        OTP otp = OTP.builder()
                .user(user)
                .email(normalizedEmail)
                .codeHash(otpUtil.hashOTPCode(code))
                .expiresAt(Instant.now().plusSeconds(currentExpiryMinutes() * 60))
                .maxAttempts(currentMaxAttempts())
                .build();
        otpRepository.save(otp);
        // Delivery is intentionally delegated to the notification provider in the next phase.
        log.info("OTP generated for email verification; delivery provider not configured");
    }

    @Transactional
    public void generateForPhone(User user, String phone) {
        String normalizedPhone = otpUtil.normalizePhoneE164(phone);
        String code = otpUtil.generateOTPCode();
        OTP otp = OTP.builder()
                .user(user)
                .phone(normalizedPhone)
                .codeHash(otpUtil.hashOTPCode(code))
                .expiresAt(Instant.now().plusSeconds(currentExpiryMinutes() * 60))
                .maxAttempts(currentMaxAttempts())
                .build();
        otpRepository.save(otp);
        log.info("OTP generated for phone verification; delivery provider not configured");
    }

    @Transactional
    public void verifyEmail(String email, String code) {
        if (!SIX_DIGITS.matcher(code).matches()) {
            throw new BusinessException("INVALID_OTP", "OTP code is invalid");
        }
        OTP otp = otpRepository.findByEmailAndExpiresAtAfter(email.trim().toLowerCase(), Instant.now())
                .orElseThrow(() -> new ResourceNotFoundException("OTP", "email"));
        verify(otp, code);
        otp.getUser().setEmailVerified(true);
        otp.getUser().setStatus(com.sahastra.backend.domain.enums.UserStatus.ACTIVE);
        otpRepository.save(otp);
    }

    @Transactional
    public void verifyPhone(String phone, String code) {
        if (!SIX_DIGITS.matcher(code).matches()) {
            throw new BusinessException("INVALID_OTP", "OTP code is invalid");
        }
        String normalizedPhone = otpUtil.normalizePhoneE164(phone);
        OTP otp = otpRepository.findByPhoneAndExpiresAtAfter(normalizedPhone, Instant.now())
                .orElseThrow(() -> new ResourceNotFoundException("OTP", "phone"));
        verify(otp, code);
        otp.getUser().setPhoneVerified(true);
        otpRepository.save(otp);
    }

    private void verify(OTP otp, String code) {
        if (!otp.isValid()) {
            throw new BusinessException("OTP_UNAVAILABLE", "OTP is expired, exhausted, or already verified");
        }
        otp.recordAttempt();
        if (!otpUtil.verifyOTPCode(code, otp.getCodeHash())) {
            otpRepository.save(otp);
            throw new BusinessException("INVALID_OTP", "OTP code is invalid");
        }
        otp.markVerified();
    }

    private long currentExpiryMinutes() {
        return systemSettingService == null ? expiryMinutes : systemSettingService.getLong(SystemSettingService.OTP_EXPIRY_MINUTES);
    }

    private int currentMaxAttempts() {
        return systemSettingService == null ? maxAttempts : Math.toIntExact(systemSettingService.getLong(SystemSettingService.OTP_MAX_ATTEMPTS));
    }
}
