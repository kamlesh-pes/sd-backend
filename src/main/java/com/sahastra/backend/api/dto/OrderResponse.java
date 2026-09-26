package com.sahastra.backend.api.dto;

import com.sahastra.backend.domain.enums.OrderStatus;
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
public class OrderResponse {
    private UUID id;
    private UUID userId;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal total;
    private String shippingAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Instant createdAt;
    private List<OrderItemResponse> items;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class OrderItemResponse {
    private UUID productId;
    private String productName;
    private String sku;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
