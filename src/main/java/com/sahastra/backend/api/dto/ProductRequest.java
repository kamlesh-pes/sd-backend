package com.sahastra.backend.api.dto;

import com.sahastra.backend.domain.enums.DiscountType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    @NotBlank @Size(max = 64)
    private String sku;
    @NotBlank @Size(max = 200)
    private String name;
    @NotBlank
    private String description;
    @NotNull
    private UUID categoryId;
    @NotNull
    private UUID brandId;
    @NotNull @DecimalMin(value = "0.01") @DecimalMax(value = "99999999.99")
    private BigDecimal basePrice;
    private DiscountType discountType;
    @DecimalMin(value = "0.00")
    private BigDecimal discountValue;
    private Instant discountStartsAt;
    private Instant discountEndsAt;
    @PositiveOrZero
    private int stock;
    private Map<String, Object> attributes;
    private Map<String, Object> images;
    private boolean active = true;
}
