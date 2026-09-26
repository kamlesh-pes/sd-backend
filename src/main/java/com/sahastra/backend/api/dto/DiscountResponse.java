package com.sahastra.backend.api.dto;

import com.sahastra.backend.domain.enums.DiscountType;
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
public class DiscountResponse {
    private UUID productId;
    private DiscountType type;
    private BigDecimal value;
    private Instant startsAt;
    private Instant endsAt;
    private boolean active;
}
