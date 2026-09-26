package com.sahastra.backend.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT token utility for generating and validating JWT tokens.
 * Implements secure token generation with proper claims and expiry.
 */
@Component
@Slf4j
public class JwtTokenUtil {

    private final SecretKey jwtSecret;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtTokenUtil(
            @Value("${security.jwt.secret:ChangeMe!ChangeMe!ChangeMe!ChangeMe!ChangeMe!ChangeMe!ChangeMe!ChangeMe!}") String secret,
            @Value("${security.jwt.access-token-expiry-minutes:15}") long accessTokenExpiryMinutes,
            @Value("${security.jwt.refresh-token-expiry-days:7}") long refreshTokenExpiryDays) {
        
        this.jwtSecret = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMinutes * 60 * 1000;
        this.refreshTokenExpiryMs = refreshTokenExpiryDays * 24 * 60 * 60 * 1000;
    }

    /**
     * Generate access token for user.
     */
    public String generateAccessToken(UUID userId, String email, boolean isAdmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("isAdmin", isAdmin);
        return generateToken(claims, userId.toString(), accessTokenExpiryMs);
    }

    /**
     * Generate refresh token for user with token family for rotation.
     */
    public String generateRefreshToken(UUID userId, String tokenFamily) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenFamily", tokenFamily);
        claims.put("type", "REFRESH");
        return generateToken(claims, userId.toString(), refreshTokenExpiryMs);
    }

    private String generateToken(Map<String, Object> claims, String subject, long expiryMs) {
        Instant now = Instant.now();
        Instant expiryInstant = now.plusMillis(expiryMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryInstant))
                .signWith(jwtSecret)
                .compact();
    }

    /**
     * Validate token and return claims if valid.
     */
    public Claims validateAndGetClaims(String token) throws SignatureException, ExpiredJwtException, 
            UnsupportedJwtException, MalformedJwtException, IllegalArgumentException {
        try {
                return Jwts.parser()
                    .verifyWith(jwtSecret)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract user ID from token.
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = validateAndGetClaims(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract email from token.
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = validateAndGetClaims(token);
            return claims.get("email", String.class);
        } catch (Exception e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract token family from refresh token.
     */
    public String getTokenFamilyFromToken(String token) {
        try {
            Claims claims = validateAndGetClaims(token);
            return claims.get("tokenFamily", String.class);
        } catch (Exception e) {
            log.error("Failed to extract token family from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateAndGetClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.error("Failed to check token expiry: {}", e.getMessage());
            return true;
        }
    }
}
