package com.sahastra.backend.domain.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * OpenSearch document for product search.
 * Maps to the "products" index for full-text search, filtering, and suggestions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private String id;

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("category_id")
    private String categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("category_slug")
    private String categorySlug;

    @JsonProperty("brand_id")
    private String brandId;

    @JsonProperty("brand_name")
    private String brandName;

    @JsonProperty("brand_slug")
    private String brandSlug;

    @JsonProperty("base_price")
    private BigDecimal basePrice;

    @JsonProperty("effective_price")
    private BigDecimal effectivePrice;

    @JsonProperty("discount_type")
    private String discountType;

    @JsonProperty("discount_value")
    private BigDecimal discountValue;

    @JsonProperty("discount_starts_at")
    private Instant discountStartsAt;

    @JsonProperty("discount_ends_at")
    private Instant discountEndsAt;

    @JsonProperty("stock")
    private int stock;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("suggest")
    private SuggestField suggest;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestField {
        @JsonProperty("input")
        private String[] input;

        @JsonProperty("weight")
        private int weight;
    }
}
