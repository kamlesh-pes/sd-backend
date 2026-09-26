package com.sahastra.backend.api.dto;

import com.sahastra.backend.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID orderId;
    private String provider;
    private String paymentId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private Instant processedAt;
    private OrderStatus orderStatus;
}
