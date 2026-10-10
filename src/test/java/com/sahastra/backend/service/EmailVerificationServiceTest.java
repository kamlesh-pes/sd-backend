package com.sahastra.backend.service;

import com.sahastra.backend.domain.entity.EmailVerificationToken;
import com.sahastra.backend.domain.entity.User;
import com.sahastra.backend.domain.enums.UserStatus;
import com.sahastra.backend.domain.repository.EmailVerificationTokenRepository;
import com.sahastra.backend.domain.repository.UserRepository;
import com.sahastra.backend.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(
                tokenRepository,
                userRepository,
                mailSender,
                "info@sahastradigital.com",
                "http://localhost:8080/api/v1/auth/verify-email",
                24);
    }

    @Test
    void sendsLinkAndStoresOnlyTokenHash() throws Exception {
        User user = User.builder()
                .email("customer@example.com")
                .firstName("Jane")
                .build();

        emailVerificationService.sendVerificationEmail(user);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(mailSender).send(messageCaptor.capture());
        verify(tokenRepository).save(tokenCaptor.capture());

        String text = messageCaptor.getValue().getText();
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(text);
        assertTrue(matcher.find());
        String rawToken = matcher.group(1);
        assertNotEquals(rawToken, tokenCaptor.getValue().getTokenHash());
        assertEquals(hashToken(rawToken), tokenCaptor.getValue().getTokenHash());
        assertEquals("customer@example.com", messageCaptor.getValue().getTo()[0]);
        assertTrue(tokenCaptor.getValue().getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void verifiesTokenAndActivatesUser() throws Exception {
        String rawToken = "verification-token";
        User user = User.builder()
                .email("customer@example.com")
                .status(UserStatus.INACTIVE)
                .emailVerified(false)
                .build();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(tokenRepository.findByTokenHashAndExpiresAtAfterAndConsumedAtIsNull(
                eq(hashToken(rawToken)), any(Instant.class))).thenReturn(Optional.of(token));

        emailVerificationService.verify(rawToken);

        assertTrue(user.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(token.getConsumedAt() != null);
        verify(userRepository).save(user);
    }

    @Test
    void rejectsUnknownToken() throws Exception {
        String rawToken = "unknown-token";
        when(tokenRepository.findByTokenHashAndExpiresAtAfterAndConsumedAtIsNull(
                eq(hashToken(rawToken)), any(Instant.class))).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> emailVerificationService.verify(rawToken));
    }

    @Test
    void reportsMailDeliveryFailureWithoutExposingSuccessfulDelivery() {
        User user = User.builder()
                .email("customer@example.com")
                .firstName("Jane")
                .build();
        doThrow(new MailSendException("SMTP authentication failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(BusinessException.class, () -> emailVerificationService.sendVerificationEmail(user));
    }

    private String hashToken(String rawToken) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
