package com.sahastra.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OTP verification request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OTPVerificationRequest {
    
    @NotBlank(message = "OTP code is required")
    private String code;
    
    private String phone;
    private String email;
}
