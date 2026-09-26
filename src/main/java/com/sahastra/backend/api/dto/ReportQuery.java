package com.sahastra.backend.api.dto;

import com.sahastra.backend.domain.enums.OrderStatus;

import java.time.Instant;

public record ReportQuery(Instant from, Instant to, OrderStatus status) {
}
