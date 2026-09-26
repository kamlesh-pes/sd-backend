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
public class CancellationReportResponse {
    private UUID orderId;
    private String reason;
    private String actorId;
    private String actorType;
    private Instant createdAt;
}
