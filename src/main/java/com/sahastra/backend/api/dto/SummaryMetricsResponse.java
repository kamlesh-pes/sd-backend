package com.sahastra.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryMetricsResponse {
    private Instant from;
    private Instant to;
    private long orderCount;
    private long cancelledOrderCount;
    private long unitsSold;
    private BigDecimal revenue;
    private BigDecimal averageOrderValue;
}
