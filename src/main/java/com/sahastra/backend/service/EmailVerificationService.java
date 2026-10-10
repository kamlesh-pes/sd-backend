package com.sahastra.backend.service;

import com.sahastra.backend.domain.entity.EmailVerificationToken;
import com.sahastra.backend.domain.entity.User;
import com.sahastra.backend.domain.enums.UserStatus;
import com.sahastra.backend.domain.repository.EmailVerificationTokenRepository;
import com.sahastra.backend.domain.repository.UserRepository;
import com.sahastra.backend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Slf4j
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final String senderAddress;
    private final String verificationBaseUrl;
    private final long expiryHours;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            JavaMailSender mailSender,
            @Value("${app.mail.from:info@sahastradigital.com}") String senderAddress,
            @Value("${app.email-verification.base-url:http://localhost:8080/api/v1/auth/verify-email}")
            String verificationBaseUrl,
            @Value("${app.email-verification.expiry-hours:24}") long expiryHours) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
        this.verificationBaseUrl = verificationBaseUrl;
        this.expiryHours = expiryHours;
    }

    @Transactional
    public void sendVerificationEmail(User user) {
        String rawToken = generateToken();
        Instant now = Instant.now();
        tokenRepository.save(EmailVerificationToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(now.plus(expiryHours, ChronoUnit.HOURS))
                .build());

        String verificationLink = UriComponentsBuilder.fromUriString(verificationBaseUrl)
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(user.getEmail());
        message.setSubject("Verify your Sahastra Digital email");
        message.setText("Hello " + user.getFirstName() + ",\n\n"
                + "Please verify your email address by opening this link:\n"
                + verificationLink + "\n\n"
                + "This link expires in " + expiryHours + " hours and can only be used once.\n\n"
                + "If you did not create this account, you can ignore this message.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Email verification message could not be sent");
            throw new BusinessException("EMAIL_DELIVERY_FAILED",
                    "Could not send the email verification message. Please try again later.");
        }
    }

    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }

        EmailVerificationToken token = tokenRepository
                .findByTokenHashAndExpiresAtAfterAndConsumedAtIsNull(hashToken(rawToken), Instant.now())
                .orElseThrow(this::invalidToken);

        Instant now = Instant.now();
        if (!token.isUsableAt(now)) {
            throw invalidToken();
        }
        token.consume(now);
        User user = token.getUser();
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private BusinessException invalidToken() {
        return new BusinessException("INVALID_EMAIL_VERIFICATION_TOKEN",
                "The email verification link is invalid, expired, or already used.");
    }
}
