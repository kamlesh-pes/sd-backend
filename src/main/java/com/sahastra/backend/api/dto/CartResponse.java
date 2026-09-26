package com.sahastra.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private UUID cartId;
    private UUID userId;
    private String guestSessionId;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal total;
    private Instant updatedAt;
}
