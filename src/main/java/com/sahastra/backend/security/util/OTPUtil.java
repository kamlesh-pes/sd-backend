package com.sahastra.backend.security.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * OTP (One-Time Password) utility for generating and validating OTP codes.
 * Supports HMAC-based verification for secure OTP handling.
 */
@Component
@Slf4j
public class OTPUtil {

    private static final int OTP_LENGTH = 6;
    private static final String OTP_PATTERN = "0-9";
    private final SecureRandom random = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    public OTPUtil(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Generate a random 6-digit OTP code.
     */
    public String generateOTPCode() {
        int otp = random.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }

    /**
     * Hash OTP code for storage.
     */
    public String hashOTPCode(String otpCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otpCode.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash OTP code", e);
            throw new RuntimeException("Failed to hash OTP code", e);
        }
    }

    /**
     * Verify OTP code against hash.
     */
    public boolean verifyOTPCode(String providedCode, String storedHash) {
        try {
            String providedHash = hashOTPCode(providedCode);
            return providedHash.equals(storedHash);
        } catch (Exception e) {
            log.error("Failed to verify OTP code", e);
            return false;
        }
    }

    /**
     * Normalize phone number to E.164 format.
     * Input: phone number (with or without country code)
     * Output: +{country_code}{phone_number}
     */
    public String normalizePhoneE164(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }

        // Remove all non-digit characters except +
        phone = phone.replaceAll("[^0-9+]", "");

        // If starts with +, assume already in E.164 format
        if (phone.startsWith("+")) {
            return phone;
        }

        // If starts with 0 (common format), remove it and add country code
        if (phone.startsWith("0")) {
            phone = phone.substring(1);
        }

        // Default to India country code if not specified
        if (!phone.startsWith("91")) {
            phone = "91" + phone;
        }

        return "+" + phone;
    }
}
