package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.SystemSettingResponse;
import com.sahastra.backend.api.dto.SystemSettingUpdateRequest;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.domain.entity.AuditLog;
import com.sahastra.backend.domain.entity.SystemSetting;
import com.sahastra.backend.domain.repository.AuditLogRepository;
import com.sahastra.backend.domain.repository.SystemSettingRepository;
import com.sahastra.backend.exception.BusinessException;
import com.sahastra.backend.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemSettingService {
    public static final String ACCESS_TOKEN_EXPIRY_MINUTES = "security.jwt.access-token-expiry-minutes";
    public static final String CUSTOMER_REFRESH_TOKEN_EXPIRY_DAYS = "security.jwt.refresh-token-expiry-days";
    public static final String REMEMBER_ME_TOKEN_EXPIRY_DAYS = "security.jwt.remember-me-expiry-days";
    public static final String ADMIN_REFRESH_TOKEN_EXPIRY_HOURS = "security.jwt.admin-refresh-token-expiry-hours";
    public static final String ABSOLUTE_TOKEN_CAP_DAYS = "security.jwt.absolute-token-cap-days";
    public static final String IDLE_TIMEOUT_DAYS = "security.session.idle-timeout-days";
    public static final String OTP_EXPIRY_MINUTES = "security.otp.expiry-minutes";
    public static final String OTP_MAX_ATTEMPTS = "security.otp.max-attempts";
    public static final String LOGIN_RATE_LIMIT_PER_MINUTE = "security.rate-limit.login-per-minute";
    public static final String OTP_RATE_LIMIT_PER_MINUTE = "security.rate-limit.otp-per-minute";
    public static final String API_RATE_LIMIT_PER_MINUTE = "security.rate-limit.api-per-minute";

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private final SystemSettingRepository settingRepository;
    private final AuditLogRepository auditLogRepository;

    public SystemSettingService(SystemSettingRepository settingRepository, AuditLogRepository auditLogRepository) {
        this.settingRepository = settingRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Cacheable(cacheNames = "system-settings", key = "#key")
    @Transactional(readOnly = true)
    public String get(String key) {
        Definition definition = definition(key);
        return settingRepository.findBySettingKey(key).map(SystemSetting::getSettingValue)
                .orElse(definition.defaultValue());
    }

    public long getLong(String key) {
        return Long.parseLong(get(key));
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> list() {
        List<SystemSettingResponse> result = new ArrayList<>();
        for (String key : DEFINITIONS.keySet()) {
            result.add(toResponse(settingRepository.findBySettingKey(key).orElseGet(() -> defaultEntity(key))));
        }
        return result;
    }

    @Transactional
    @CacheEvict(cacheNames = "system-settings", key = "#key")
    public SystemSettingResponse update(String key, SystemSettingUpdateRequest request, String actorId) {
        Definition definition = definition(key);
        validateValue(definition, request == null ? null : request.getValue());
        SystemSetting setting = settingRepository.findBySettingKey(key).orElseGet(() -> defaultEntity(key));
        String oldValue = setting.getSettingValue();
        setting.setSettingValue(request.getValue().trim());
        SystemSetting saved = settingRepository.save(setting);
        auditLogRepository.save(AuditLog.builder()
                .action("SYSTEM_SETTING_UPDATED")
                .actorId(actorId)
                .actorType("ADMIN")
                .targetEntity("SystemSetting")
                .targetId(key)
                .changes("{\"oldValue\":\"" + oldValue + "\",\"newValue\":\"" + saved.getSettingValue() + "\"}")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
        return toResponse(saved);
    }

    public void validateValue(String key, String value) {
        validateValue(definition(key), value);
    }

    private void validateValue(Definition definition, String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("INVALID_SETTING_VALUE", "Setting value is required");
        }
        try {
            BigDecimal numeric = new BigDecimal(value.trim());
            BigDecimal min = new BigDecimal(definition.minValue());
            BigDecimal max = new BigDecimal(definition.maxValue());
            if (numeric.compareTo(min) < 0 || numeric.compareTo(max) > 0 || numeric.stripTrailingZeros().scale() > 0) {
                throw new BusinessException("SETTING_OUT_OF_RANGE", "Setting value is outside its allowed bounds");
            }
        } catch (NumberFormatException exception) {
            throw new BusinessException("INVALID_SETTING_VALUE", "Setting value must be an integer");
        }
    }

    private Definition definition(String key) {
        Definition definition = DEFINITIONS.get(key);
        if (definition == null) {
            throw new ResourceNotFoundException("System setting", key);
        }
        return definition;
    }

    private SystemSetting defaultEntity(String key) {
        Definition definition = definition(key);
        return SystemSetting.builder()
                .settingKey(key)
                .settingValue(definition.defaultValue())
                .description(definition.description())
                .valueType("INTEGER")
                .minValue(definition.minValue())
                .maxValue(definition.maxValue())
                .build();
    }

    private SystemSettingResponse toResponse(SystemSetting setting) {
        return SystemSettingResponse.builder()
                .key(setting.getSettingKey())
                .value(setting.getSettingValue())
                .description(setting.getDescription())
                .valueType(setting.getValueType())
                .minValue(setting.getMinValue())
                .maxValue(setting.getMaxValue())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    private static Map<String, Definition> definitions() {
        Map<String, Definition> definitions = new LinkedHashMap<>();
        definitions.put(ACCESS_TOKEN_EXPIRY_MINUTES, new Definition("15", "Access token lifetime in minutes", "1", "60"));
        definitions.put(CUSTOMER_REFRESH_TOKEN_EXPIRY_DAYS, new Definition("7", "Customer refresh token lifetime in days", "1", "30"));
        definitions.put(REMEMBER_ME_TOKEN_EXPIRY_DAYS, new Definition("30", "Remember-me token lifetime in days", "1", "30"));
        definitions.put(ADMIN_REFRESH_TOKEN_EXPIRY_HOURS, new Definition("8", "Admin refresh token lifetime in hours", "1", "24"));
        definitions.put(ABSOLUTE_TOKEN_CAP_DAYS, new Definition("30", "Maximum token lifetime in days", "1", "30"));
        definitions.put(IDLE_TIMEOUT_DAYS, new Definition("14", "Session idle timeout in days", "1", "30"));
        definitions.put(OTP_EXPIRY_MINUTES, new Definition("5", "OTP lifetime in minutes", "1", "15"));
        definitions.put(OTP_MAX_ATTEMPTS, new Definition("5", "Maximum OTP verification attempts", "1", "10"));
        definitions.put(LOGIN_RATE_LIMIT_PER_MINUTE, new Definition("5", "Login attempts allowed per minute", "1", "20"));
        definitions.put(OTP_RATE_LIMIT_PER_MINUTE, new Definition("5", "OTP and password-reset requests allowed per minute", "1", "20"));
        definitions.put(API_RATE_LIMIT_PER_MINUTE, new Definition("120", "API requests allowed per minute per client address", "10", "1000"));
        return Map.copyOf(definitions);
    }

    private record Definition(String defaultValue, String description, String minValue, String maxValue) {
    }
}
