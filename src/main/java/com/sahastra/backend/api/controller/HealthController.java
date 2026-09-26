package com.sahastra.backend.api.controller;

import com.sahastra.backend.api.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint for monitoring service status.
 * Accessible without authentication.
 */
@RestController
@RequestMapping("/api/v1/health")
@Slf4j
public class HealthController {

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, String>>> ping() {
        log.info("Health check ping received");
        
        Map<String, String> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "sahastra-backend");
        data.put("message", "Service is running");
        
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .data(data)
                .message("Service is healthy")
                .build());
    }

    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<Map<String, String>>> ready() {
        log.info("Readiness check received");
        
        Map<String, String> data = new HashMap<>();
        data.put("status", "READY");
        data.put("service", "sahastra-backend");
        
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .data(data)
                .message("Service is ready to accept requests")
                .build());
    }
}
