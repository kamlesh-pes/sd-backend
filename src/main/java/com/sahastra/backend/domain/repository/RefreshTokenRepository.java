package com.sahastra.backend.domain.repository;

import com.sahastra.backend.domain.entity.RefreshToken;
import com.sahastra.backend.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity.
 * Manages refresh tokens with rotating token family strategy.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    List<RefreshToken> findByUserAndTokenFamily(User user, String tokenFamily);
    
    List<RefreshToken> findByUser(User user);
    
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = ?1 AND rt.expiresAt > ?2")
    List<RefreshToken> findValidTokensByUser(User user, Instant now);
    
    void deleteByExpiresAtBefore(Instant date);
}
