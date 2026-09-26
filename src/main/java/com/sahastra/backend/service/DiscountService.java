package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.DiscountRequest;
import com.sahastra.backend.api.dto.DiscountResponse;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.domain.entity.AuditLog;
import com.sahastra.backend.domain.entity.Product;
import com.sahastra.backend.domain.enums.DiscountType;
import com.sahastra.backend.domain.repository.AuditLogRepository;
import com.sahastra.backend.domain.repository.ProductRepository;
import com.sahastra.backend.exception.BusinessException;
import com.sahastra.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class DiscountService {
    private final ProductRepository productRepository;
    private final AuditLogRepository auditLogRepository;

    public DiscountService(ProductRepository productRepository, AuditLogRepository auditLogRepository) {
        this.productRepository = productRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public DiscountResponse create(UUID productId, DiscountRequest request, String actorId) {
        Product product = findProduct(productId);
        validate(request, product.getBasePrice());
        if (product.getDiscountType() != null) {
            throw new BusinessException("DISCOUNT_ALREADY_EXISTS", "Product already has a discount");
        }
        apply(product, request);
        Product saved = productRepository.save(product);
        audit("DISCOUNT_CREATED", actorId, saved, changes(request));
        return toResponse(saved);
    }

    @Transactional
    public DiscountResponse update(UUID productId, DiscountRequest request, String actorId) {
        Product product = findProduct(productId);
        validate(request, product.getBasePrice());
        apply(product, request);
        Product saved = productRepository.save(product);
        audit("DISCOUNT_UPDATED", actorId, saved, changes(request));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DiscountResponse get(UUID productId) {
        Product product = findProduct(productId);
        if (product.getDiscountType() == null) {
            throw new ResourceNotFoundException("Discount", productId.toString());
        }
        return toResponse(product);
    }

    @Transactional
    public void delete(UUID productId, String actorId) {
        Product product = findProduct(productId);
        if (product.getDiscountType() == null) {
            throw new ResourceNotFoundException("Discount", productId.toString());
        }
        product.setDiscountType(null);
        product.setDiscountValue(null);
        product.setDiscountStartsAt(null);
        product.setDiscountEndsAt(null);
        Product saved = productRepository.save(product);
        audit("DISCOUNT_DELETED", actorId, saved, "{\"discountRemoved\":true}");
    }

    private Product findProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));
    }

    private void apply(Product product, DiscountRequest request) {
        product.setDiscountType(request.getType());
        product.setDiscountValue(request.getValue().setScale(2));
        product.setDiscountStartsAt(request.getStartsAt());
        product.setDiscountEndsAt(request.getEndsAt());
    }

    private void validate(DiscountRequest request, BigDecimal basePrice) {
        if (request == null || request.getType() == null || request.getValue() == null) {
            throw new BusinessException("INVALID_DISCOUNT", "Discount type and value are required");
        }
        if (request.getValue().signum() <= 0) {
            throw new BusinessException("INVALID_DISCOUNT", "Discount value must be positive");
        }
        if (request.getType() == DiscountType.PERCENTAGE && request.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("INVALID_DISCOUNT", "Percentage discount cannot exceed 100");
        }
        if (request.getType() == DiscountType.FIXED && request.getValue().compareTo(basePrice) > 0) {
            throw new BusinessException("INVALID_DISCOUNT", "Fixed discount cannot exceed base price");
        }
        if (request.getStartsAt() != null && request.getEndsAt() != null
                && !request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new BusinessException("INVALID_DISCOUNT_WINDOW", "Discount end must be after discount start");
        }
    }

    private DiscountResponse toResponse(Product product) {
        Instant now = Instant.now();
        return DiscountResponse.builder()
                .productId(product.getId())
                .type(product.getDiscountType())
                .value(product.getDiscountValue())
                .startsAt(product.getDiscountStartsAt())
                .endsAt(product.getDiscountEndsAt())
                .active(product.hasActiveDiscount(now))
                .build();
    }

    private String changes(DiscountRequest request) {
        return "{\"type\":\"" + request.getType() + "\",\"value\":\"" + request.getValue()
                + "\",\"startsAt\":\"" + request.getStartsAt() + "\",\"endsAt\":\"" + request.getEndsAt() + "\"}";
    }

    private void audit(String action, String actorId, Product product, String changes) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .actorId(actorId)
                .actorType("ADMIN")
                .targetEntity("Product")
                .targetId(product.getId().toString())
                .changes(changes)
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }
}
