package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.AuthResponse;
import com.sahastra.backend.api.dto.LoginRequest;
import com.sahastra.backend.api.dto.RegisterRequest;
import com.sahastra.backend.api.dto.UserDTO;
import com.sahastra.backend.domain.entity.RefreshToken;
import com.sahastra.backend.domain.entity.User;
import com.sahastra.backend.domain.enums.UserRole;
import com.sahastra.backend.domain.enums.UserStatus;
import com.sahastra.backend.domain.repository.RefreshTokenRepository;
import com.sahastra.backend.domain.repository.UserRepository;
import com.sahastra.backend.exception.BusinessException;
import com.sahastra.backend.exception.ResourceNotFoundException;
import com.sahastra.backend.exception.ValidationException;
import com.sahastra.backend.security.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authentication service for user registration, login, and token management.
 * Implements secure password hashing and JWT token strategy with rotating refresh tokens.
 */
@Service
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final OTPService otpService;
    private final SystemSettingService systemSettingService;

    @Autowired
    public AuthenticationService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenUtil jwtTokenUtil,
            OTPService otpService,
            SystemSettingService systemSettingService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.otpService = otpService;
        this.systemSettingService = systemSettingService;
    }

    /**
     * Register a new user.
     */
    @Transactional
    public UserDTO register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Validate email is not already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email already exists: {}", request.getEmail());
                throw new ValidationException("Email already registered", 
                    new Object[]{"email", request.getEmail()});
        }

        // Validate phone if provided
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            if (userRepository.existsByPhone(request.getPhone())) {
                log.warn("Registration failed: phone already exists: {}", request.getPhone());
                throw new ValidationException("Phone already registered",
                        new Object[]{"phone", request.getPhone()});
            }
        }

        // Create new user with hashed password
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.INACTIVE)
                .roles(java.util.Set.of(UserRole.ROLE_CUSTOMER))
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());

        otpService.generateForEmail(savedUser, savedUser.getEmail());

        // Audit log
        auditLog("USER_REGISTERED", savedUser.getId(), "User", savedUser.getId().toString());

        return mapUserToDTO(savedUser);
    }

    /**
     * Login user with email and password.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt with email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found: {}", request.getEmail());
                    return new ResourceNotFoundException("User", request.getEmail());
                });

        // Check account status
        if (!user.isActive()) {
            log.warn("Login failed: account not active: {}", user.getId());
            throw new BusinessException("ACCOUNT_INACTIVE", 
                    "Account is inactive. Please verify your email.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password for user: {}", user.getId());
            auditLog("LOGIN_FAILED", user.getId(), "User", user.getId().toString());
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        // Generate tokens
        String accessToken = jwtTokenUtil.generateAccessToken(user.getId(), user.getEmail(), user.isAdmin());
        String tokenFamily = UUID.randomUUID().toString();
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getId(), tokenFamily);

        // Store hashed refresh token
        saveRefreshToken(user, refreshToken, tokenFamily);

        // Update last login time
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getId());
        auditLog("LOGIN_SUCCESS", user.getId(), "User", user.getId().toString());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(Math.toIntExact(systemSettingService.getLong(SystemSettingService.ACCESS_TOKEN_EXPIRY_MINUTES) * 60))
                .user(mapUserToDTO(user))
                .build();
    }

    /**
     * Refresh access token using refresh token.
     */
    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        log.info("Token refresh requested");

        // Validate and get claims from refresh token
        try {
            jwtTokenUtil.validateAndGetClaims(refreshTokenStr);
        } catch (Exception e) {
            log.warn("Invalid refresh token");
            throw new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
        }

        // Hash the refresh token to look up in database
        String tokenHash = hashToken(refreshTokenStr);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in database");
                    return new ResourceNotFoundException("Refresh token", "not found");
                });

        // Check if token is valid
        if (!refreshToken.isValid()) {
            log.warn("Refresh token is invalid or expired");
            if (refreshToken.isRevoked() || refreshToken.isCompromised()) {
                // Replay of a rotated token indicates possible token theft.
                revokeTokenFamily(refreshToken.getUser(), refreshToken.getTokenFamily());
                auditLog("TOKEN_FAMILY_REVOKED", refreshToken.getUser().getId(), "User", 
                        refreshToken.getUser().getId().toString());
            }
            throw new BusinessException("REFRESH_TOKEN_EXPIRED", "Refresh token is no longer valid");
        }

        User user = refreshToken.getUser();

        // Generate new tokens with new token family (rotate)
        String newAccessToken = jwtTokenUtil.generateAccessToken(user.getId(), user.getEmail(), user.isAdmin());
        String newTokenFamily = UUID.randomUUID().toString();
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(user.getId(), newTokenFamily);

        // Revoke old refresh token
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        // Store new hashed refresh token
        saveRefreshToken(user, newRefreshToken, newTokenFamily);

        log.info("Token refreshed successfully for user: {}", user.getId());
        auditLog("TOKEN_REFRESHED", user.getId(), "User", user.getId().toString());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(Math.toIntExact(systemSettingService.getLong(SystemSettingService.ACCESS_TOKEN_EXPIRY_MINUTES) * 60))
                .build();
    }

    /**
     * Logout user by revoking refresh token.
     */
    @Transactional
    public void logout(UUID userId, String refreshTokenStr) {
        log.info("User logout: {}", userId);

        String tokenHash = hashToken(refreshTokenStr);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token", "not found"));

        if (!refreshToken.getUser().getId().equals(userId)) {
            throw new BusinessException("INVALID_USER", "Token does not belong to this user");
        }

        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        auditLog("LOGOUT", userId, "User", userId.toString());
        log.info("User logged out successfully: {}", userId);
    }

    /**
     * Get user by ID.
     */
    public UserDTO getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        return mapUserToDTO(user);
    }

    @Transactional
    public void verifyEmailOtp(String email, String code) {
        otpService.verifyEmail(email, code);
    }

    @Transactional
    public void verifyPhoneOtp(String phone, String code) {
        otpService.verifyPhone(phone, code);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email.trim().toLowerCase())
                .ifPresent(user -> otpService.generateForEmail(user, user.getEmail()));
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        otpService.verifyEmail(email, code);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        refreshTokenRepository.findByUser(user).forEach(token -> {
            token.revoke();
            token.setCompromised(true);
        });
        userRepository.save(user);
    }

    /**
     * Save hashed refresh token.
     */
    private void saveRefreshToken(User user, String refreshToken, String tokenFamily) {
        String tokenHash = hashToken(refreshToken);
        Instant expiresAt = Instant.now().plusSeconds(systemSettingService.getLong(SystemSettingService.CUSTOMER_REFRESH_TOKEN_EXPIRY_DAYS) * 24 * 60 * 60);

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .tokenFamily(tokenFamily)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(token);
    }

    /**
     * Revoke all tokens in a family (used when breach is detected).
     */
    private void revokeTokenFamily(User user, String tokenFamily) {
        refreshTokenRepository.findByUserAndTokenFamily(user, tokenFamily)
                .forEach(token -> {
                    token.revoke();
                    token.setCompromised(true);
                });
    }

    /**
     * Hash token for storage (SHA-256).
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    /**
     * Map User entity to UserDTO.
     */
    private UserDTO mapUserToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    /**
     * Audit log helper.
     */
    private void auditLog(String action, UUID actorId, String targetEntity, String targetId) {
        // Will be implemented in audit service
        log.debug("Audit: {} - Actor: {} - Target: {}", action, actorId, targetEntity);
    }
}
