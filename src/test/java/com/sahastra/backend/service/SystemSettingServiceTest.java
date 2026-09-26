package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.SystemSettingResponse;
import com.sahastra.backend.api.dto.SystemSettingUpdateRequest;
import com.sahastra.backend.domain.entity.AuditLog;
import com.sahastra.backend.domain.entity.SystemSetting;
import com.sahastra.backend.domain.repository.AuditLogRepository;
import com.sahastra.backend.domain.repository.SystemSettingRepository;
import com.sahastra.backend.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {
    @Mock
    private SystemSettingRepository settingRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void returnsDocumentedDefaultWhenSettingIsMissing() {
        SystemSettingService service = new SystemSettingService(settingRepository, auditLogRepository);

        when(settingRepository.findBySettingKey(SystemSettingService.OTP_EXPIRY_MINUTES)).thenReturn(Optional.empty());

        assertEquals("5", service.get(SystemSettingService.OTP_EXPIRY_MINUTES));
    }

    @Test
    void updatesSettingAndAuditsOldAndNewValues() {
        SystemSetting existing = SystemSetting.builder()
                .settingKey(SystemSettingService.OTP_MAX_ATTEMPTS)
                .settingValue("5")
                .valueType("INTEGER")
                .minValue("1")
                .maxValue("10")
                .build();
        SystemSettingService service = new SystemSettingService(settingRepository, auditLogRepository);

        when(settingRepository.findBySettingKey(SystemSettingService.OTP_MAX_ATTEMPTS)).thenReturn(Optional.of(existing));
        when(settingRepository.save(existing)).thenReturn(existing);

        SystemSettingResponse response = service.update(SystemSettingService.OTP_MAX_ATTEMPTS,
                SystemSettingUpdateRequest.builder().value("7").build(), "admin-1");

        assertEquals("7", response.getValue());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals("SYSTEM_SETTING_UPDATED", auditCaptor.getValue().getAction());
        assertEquals("{\"oldValue\":\"5\",\"newValue\":\"7\"}", auditCaptor.getValue().getChanges());
    }

    @Test
    void rejectsValuesOutsideConfiguredBounds() {
        SystemSettingService service = new SystemSettingService(settingRepository, auditLogRepository);

        assertThrows(BusinessException.class, () -> service.validateValue(SystemSettingService.OTP_MAX_ATTEMPTS, "0"));
        assertThrows(BusinessException.class, () -> service.validateValue(SystemSettingService.OTP_MAX_ATTEMPTS, "11"));
        assertThrows(BusinessException.class, () -> service.validateValue(SystemSettingService.OTP_MAX_ATTEMPTS, "2.5"));
    }

    @Test
    void rejectsUnknownSettings() {
        SystemSettingService service = new SystemSettingService(settingRepository, auditLogRepository);

        assertThrows(Exception.class, () -> service.validateValue("security.unknown", "1"));
    }
}
