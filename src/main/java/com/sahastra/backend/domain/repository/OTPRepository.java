package com.sahastra.backend.domain.repository;

import com.sahastra.backend.domain.entity.OTP;
import com.sahastra.backend.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OTP entity.
 * Manages one-time passwords for email and phone verification.
 */
@Repository
public interface OTPRepository extends JpaRepository<OTP, UUID> {
    Optional<OTP> findByUserAndPhoneAndExpiresAtAfter(User user, String phone, Instant now);
    
    Optional<OTP> findByUserAndEmailAndExpiresAtAfter(User user, String email, Instant now);
    
    Optional<OTP> findByPhoneAndExpiresAtAfter(String phone, Instant now);
    
    Optional<OTP> findByEmailAndExpiresAtAfter(String email, Instant now);
    
    long countByPhoneAndCreatedAtAfter(String phone, Instant since);
    
    long countByEmailAndCreatedAtAfter(String email, Instant since);
    
    void deleteByExpiresAtBefore(Instant date);
}
