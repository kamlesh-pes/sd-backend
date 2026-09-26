package com.sahastra.backend.api.dto;

import com.sahastra.backend.domain.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class DiscountRequest {
    @NotNull
    private DiscountType type;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal value;

    private Instant startsAt;
    private Instant endsAt;
}
