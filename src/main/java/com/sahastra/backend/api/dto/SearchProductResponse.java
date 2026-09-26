package com.sahastra.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Search result for product queries.
 * Includes relevance score from OpenSearch.
 */
@Value
@Builder
@AllArgsConstructor
public class SearchProductResponse {
    String id;
    String sku;
    String name;
    String description;
    String categoryName;
    String brandName;
    BigDecimal basePrice;
    BigDecimal effectivePrice;
    int stock;
    boolean active;
    Instant createdAt;
    double score;
}
