package com.sahastra.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * OTP (One-Time Password) entity for email and phone verification.
 * Stores hashed OTP codes with rate limiting and attempt tracking.
 */
@Entity
@Table(name = "otps", indexes = {
        @Index(name = "idx_otp_user", columnList = "user_id"),
        @Index(name = "idx_otp_phone", columnList = "phone"),
        @Index(name = "idx_otp_email", columnList = "email"),
        @Index(name = "idx_otp_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OTP {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_otp_user"))
    private User user;

    @Column(length = 20)
    private String phone; // E.164 normalized phone number

    @Column(length = 255)
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String codeHash; // Hash of the OTP code

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int maxAttempts = 5;

    @Column
    private Instant verifiedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isValid() {
        return !isExpired() && !isExhausted() && !isVerified();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isExhausted() {
        return attemptCount >= maxAttempts;
    }

    public void recordAttempt() {
        this.attemptCount++;
    }

    public void markVerified() {
        this.verifiedAt = Instant.now();
    }
}
