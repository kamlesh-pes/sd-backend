package com.sahastra.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingResponse {
    private String key;
    private String value;
    private String description;
    private String valueType;
    private String minValue;
    private String maxValue;
    private Instant updatedAt;
}
