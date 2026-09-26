package com.sahastra.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditReportResponse {
    private UUID id;
    private String action;
    private String actorId;
    private String actorType;
    private String targetEntity;
    private String targetId;
    private String changes;
    private Instant createdAt;
}
