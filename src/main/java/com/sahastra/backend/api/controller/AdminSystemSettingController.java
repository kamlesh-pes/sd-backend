package com.sahastra.backend.api.controller;

import com.sahastra.backend.api.dto.ApiResponse;
import com.sahastra.backend.api.dto.SystemSettingResponse;
import com.sahastra.backend.api.dto.SystemSettingUpdateRequest;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.service.SystemSettingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/settings")
public class AdminSystemSettingController {
    private final SystemSettingService settingService;

    public AdminSystemSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemSettingResponse>>> list() {
        return ok("Settings retrieved", settingService.list());
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSettingResponse>> update(@PathVariable String key,
                                                                      @Valid @RequestBody SystemSettingUpdateRequest request,
                                                                      Authentication authentication) {
        return ok("Setting updated", settingService.update(key, request, authentication.getName()));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }
}
