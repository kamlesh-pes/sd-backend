package com.sahastra.backend.api.controller;

import com.sahastra.backend.api.dto.AuthResponse;
import com.sahastra.backend.api.dto.ApiResponse;
import com.sahastra.backend.api.dto.LoginRequest;
import com.sahastra.backend.api.dto.OTPVerificationRequest;
import com.sahastra.backend.api.dto.PasswordResetRequest;
import com.sahastra.backend.api.dto.PasswordResetConfirmRequest;
import com.sahastra.backend.api.dto.RefreshTokenRequest;
import com.sahastra.backend.api.dto.RegisterRequest;
import com.sahastra.backend.api.dto.UserDTO;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * REST controller for authentication operations.
 * Endpoints for user registration, login, token refresh, and logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Register a new user.
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email: {}", request.getEmail());
        
        UserDTO userDTO = authenticationService.register(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserDTO>builder()
                        .success(true)
                        .data(userDTO)
                        .message("User registered successfully")
                        .correlationId(CorrelationIdContext.getCorrelationId())
                        .build()
        );
    }

    /**
     * Login user and get JWT tokens.
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        
        AuthResponse authResponse = authenticationService.login(request);
        
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .success(true)
                        .data(authResponse)
                        .message("Login successful")
                        .correlationId(CorrelationIdContext.getCorrelationId())
                        .build()
        );
    }

    /**
     * Refresh access token using refresh token.
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        
        AuthResponse authResponse = authenticationService.refreshToken(request.getRefreshToken());
        
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .success(true)
                        .data(authResponse)
                        .message("Token refreshed successfully")
                        .correlationId(CorrelationIdContext.getCorrelationId())
                        .build()
        );
    }

    /**
     * Logout user by revoking refresh token.
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication,
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Logout request");
        
        UUID userId = UUID.fromString(authentication.getName());
        
        authenticationService.logout(userId, request.getRefreshToken());
        
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Logout successful")
                        .correlationId(CorrelationIdContext.getCorrelationId())
                        .build()
        );
    }

    /**
     * Get current user profile.
     * GET /api/v1/auth/me
     * Requires authentication
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(
            Authentication authentication) {
        log.info("Get current user request");
        
        UserDTO userDTO = authenticationService.getUserById(UUID.fromString(authentication.getName()));
        
        return ResponseEntity.ok(
                ApiResponse.<UserDTO>builder()
                        .success(true)
                        .data(userDTO)
                        .message("User profile retrieved")
                        .correlationId(CorrelationIdContext.getCorrelationId())
                        .build()
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody OTPVerificationRequest request) {
        if ((request.getEmail() == null) == (request.getPhone() == null)) {
            throw new com.sahastra.backend.exception.ValidationException("Provide exactly one email or phone");
        }
        if (request.getEmail() != null) {
            authenticationService.verifyEmailOtp(request.getEmail(), request.getCode());
        } else {
            authenticationService.verifyPhoneOtp(request.getPhone(), request.getCode());
        }
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("OTP verified successfully")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        authenticationService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("If the account exists, a password reset OTP has been sent")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        authenticationService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Password reset successfully")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }
}
