package com.sahastra.backend.api.dto;

import com.sahastra.backend.domain.enums.DiscountType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class ProductResponse {
    UUID id;
    String sku;
    String name;
    String description;
    UUID categoryId;
    String categoryName;
    UUID brandId;
    String brandName;
    BigDecimal basePrice;
    DiscountType discountType;
    BigDecimal discountValue;
    Instant discountStartsAt;
    Instant discountEndsAt;
    BigDecimal effectivePrice;
    int stock;
    Map<String, Object> attributes;
    Map<String, Object> images;
    boolean active;
    long version;
    Instant createdAt;
    Instant updatedAt;
}
