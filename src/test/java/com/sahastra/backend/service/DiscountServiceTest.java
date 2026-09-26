package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.DiscountRequest;
import com.sahastra.backend.api.dto.DiscountResponse;
import com.sahastra.backend.domain.entity.AuditLog;
import com.sahastra.backend.domain.entity.Product;
import com.sahastra.backend.domain.enums.DiscountType;
import com.sahastra.backend.domain.repository.AuditLogRepository;
import com.sahastra.backend.domain.repository.ProductRepository;
import com.sahastra.backend.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void createsPercentageDiscountAndAuditsChange() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId);
        DiscountService service = new DiscountService(productRepository, auditLogRepository);
        DiscountRequest request = DiscountRequest.builder()
                .type(DiscountType.PERCENTAGE)
                .value(new BigDecimal("20"))
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        DiscountResponse response = service.create(productId, request, "admin-1");

        assertEquals(DiscountType.PERCENTAGE, response.getType());
        assertEquals(new BigDecimal("20.00"), response.getValue());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void rejectsSecondDiscountOnSameProduct() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId);
        product.setDiscountType(DiscountType.FIXED);
        product.setDiscountValue(new BigDecimal("5.00"));
        DiscountService service = new DiscountService(productRepository, auditLogRepository);
        DiscountRequest request = DiscountRequest.builder()
                .type(DiscountType.PERCENTAGE)
                .value(new BigDecimal("20"))
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThrows(BusinessException.class, () -> service.create(productId, request, "admin-1"));
    }

    @Test
    void rejectsPercentageAboveOneHundred() {
        UUID productId = UUID.randomUUID();
        DiscountService service = new DiscountService(productRepository, auditLogRepository);
        DiscountRequest request = DiscountRequest.builder()
                .type(DiscountType.PERCENTAGE)
                .value(new BigDecimal("100.01"))
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product(productId)));

        assertThrows(BusinessException.class, () -> service.create(productId, request, "admin-1"));
    }

    @Test
    void expiredDiscountIsReturnedAsInactive() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId);
        product.setDiscountType(DiscountType.FIXED);
        product.setDiscountValue(new BigDecimal("5.00"));
        product.setDiscountEndsAt(Instant.now().minusSeconds(60));
        DiscountService service = new DiscountService(productRepository, auditLogRepository);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        DiscountResponse response = service.get(productId);

        assertFalse(response.isActive());
    }

    @Test
    void rejectsInvalidDiscountWindow() {
        UUID productId = UUID.randomUUID();
        DiscountService service = new DiscountService(productRepository, auditLogRepository);
        Instant start = Instant.parse("2026-09-26T10:00:00Z");
        DiscountRequest request = DiscountRequest.builder()
                .type(DiscountType.FIXED)
                .value(new BigDecimal("5"))
                .startsAt(start)
                .endsAt(start)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product(productId)));

        assertThrows(BusinessException.class, () -> service.create(productId, request, "admin-1"));
    }

    private Product product(UUID id) {
        return Product.builder()
                .id(id)
                .sku("SKU-" + id)
                .name("Notebook")
                .description("Desk notebook")
                .basePrice(new BigDecimal("25.00"))
                .stock(5)
                .active(true)
                .build();
    }
}
