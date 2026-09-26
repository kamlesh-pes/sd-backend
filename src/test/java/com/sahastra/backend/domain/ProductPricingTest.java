package com.sahastra.backend.domain;

import com.sahastra.backend.domain.entity.Product;
import com.sahastra.backend.domain.enums.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductPricingTest {
    @Test
    void percentageDiscountIsRoundedAndApplied() {
        Product product = Product.builder()
                .basePrice(new BigDecimal("99.99"))
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10"))
                .discountStartsAt(Instant.now().minusSeconds(1))
                .discountEndsAt(Instant.now().plusSeconds(60))
                .build();

        assertEquals(new BigDecimal("89.99"), product.effectivePrice(Instant.now()));
    }

    @Test
    void expiredDiscountReturnsBasePrice() {
        Product product = Product.builder()
                .basePrice(new BigDecimal("20.00"))
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("5.00"))
                .discountEndsAt(Instant.now().minusSeconds(1))
                .build();

        assertEquals(new BigDecimal("20.00"), product.effectivePrice(Instant.now()));
    }
}
