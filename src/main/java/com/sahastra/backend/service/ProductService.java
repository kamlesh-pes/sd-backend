package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.ProductRequest;
import com.sahastra.backend.api.dto.ProductResponse;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.domain.entity.AuditLog;
import com.sahastra.backend.domain.entity.Brand;
import com.sahastra.backend.domain.entity.Category;
import com.sahastra.backend.domain.entity.Product;
import com.sahastra.backend.domain.enums.DiscountType;
import com.sahastra.backend.domain.repository.AuditLogRepository;
import com.sahastra.backend.domain.repository.BrandRepository;
import com.sahastra.backend.domain.repository.CategoryRepository;
import com.sahastra.backend.domain.repository.ProductRepository;
import com.sahastra.backend.event.ProductDeletedEvent;
import com.sahastra.backend.event.ProductSavedEvent;
import com.sahastra.backend.exception.BusinessException;
import com.sahastra.backend.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          BrandRepository brandRepository, AuditLogRepository auditLogRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.auditLogRepository = auditLogRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProductResponse create(ProductRequest request, String actorId) {
        String sku = normalize(request.getSku());
        if (productRepository.existsBySku(sku)) {
            throw new BusinessException("SKU_ALREADY_EXISTS", "SKU is already in use");
        }
        Product product = new Product();
        apply(product, request, sku);
        Product saved = productRepository.save(product);
        audit("PRODUCT_CREATED", actorId, saved, "{\"sku\":\"" + sku + "\"}");
        eventPublisher.publishEvent(new ProductSavedEvent(this, saved));
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request, String actorId) {
        Product product = find(id);
        String sku = normalize(request.getSku());
        if (!sku.equals(product.getSku()) && productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new BusinessException("SKU_ALREADY_EXISTS", "SKU is already in use");
        }
        validateDiscount(request);
        apply(product, request, sku);
        Product saved = productRepository.save(product);
        audit("PRODUCT_UPDATED", actorId, saved, "{\"sku\":\"" + saved.getSku() + "\",\"basePrice\":\"" + saved.getBasePrice() + "\"}");
        eventPublisher.publishEvent(new ProductSavedEvent(this, saved));
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id, String actorId) {
        Product product = find(id);
        product.setActive(false);
        productRepository.save(product);
        audit("PRODUCT_DEACTIVATED", actorId, product, "{\"active\":false}");
        eventPublisher.publishEvent(new ProductDeletedEvent(this, id.toString()));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        Product product = find(id);
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product", id.toString());
        }
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(String category, String brand, Pageable pageable, boolean includeInactive) {
        Page<Product> products;
        if (includeInactive) {
            products = productRepository.findAll(pageable);
        } else if (category != null && !category.isBlank()) {
            products = productRepository.findByActiveTrueAndCategorySlug(category.trim().toLowerCase(), pageable);
        } else if (brand != null && !brand.isBlank()) {
            products = productRepository.findByActiveTrueAndBrandSlug(brand.trim().toLowerCase(), pageable);
        } else {
            products = productRepository.findByActiveTrue(pageable);
        }
        return products.map(this::toResponse);
    }

    private Product find(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
    }

    private void apply(Product product, ProductRequest request, String sku) {
        validateDiscount(request);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId().toString()));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand", request.getBrandId().toString()));
        product.setSku(sku);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription().trim());
        product.setCategory(category);
        product.setBrand(brand);
        product.setBasePrice(request.getBasePrice().setScale(2));
        product.setDiscountType(request.getDiscountType());
        product.setDiscountValue(request.getDiscountValue() == null ? null : request.getDiscountValue().setScale(2));
        product.setDiscountStartsAt(request.getDiscountStartsAt());
        product.setDiscountEndsAt(request.getDiscountEndsAt());
        product.setStock(request.getStock());
        product.setAttributes(request.getAttributes());
        product.setImages(request.getImages());
        product.setActive(request.isActive());
    }

    private void validateDiscount(ProductRequest request) {
        if ((request.getDiscountType() == null) != (request.getDiscountValue() == null)) {
            throw new BusinessException("INVALID_DISCOUNT", "Discount type and value must be provided together");
        }
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue() != null
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("INVALID_DISCOUNT", "Percentage discount cannot exceed 100");
        }
        if (request.getDiscountType() == DiscountType.FIXED
                && request.getDiscountValue() != null
                && request.getDiscountValue().compareTo(request.getBasePrice()) > 0) {
            throw new BusinessException("INVALID_DISCOUNT", "Fixed discount cannot exceed base price");
        }
        if (request.getDiscountStartsAt() != null && request.getDiscountEndsAt() != null
                && !request.getDiscountEndsAt().isAfter(request.getDiscountStartsAt())) {
            throw new BusinessException("INVALID_DISCOUNT_WINDOW", "Discount end must be after discount start");
        }
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId()).sku(product.getSku()).name(product.getName()).description(product.getDescription())
                .categoryId(product.getCategory().getId()).categoryName(product.getCategory().getName())
                .brandId(product.getBrand().getId()).brandName(product.getBrand().getName())
                .basePrice(product.getBasePrice()).discountType(product.getDiscountType()).discountValue(product.getDiscountValue())
                .discountStartsAt(product.getDiscountStartsAt()).discountEndsAt(product.getDiscountEndsAt())
                .effectivePrice(product.effectivePrice(Instant.now())).stock(product.getStock())
                .attributes(product.getAttributes()).images(product.getImages()).active(product.isActive())
                .version(product.getVersion()).createdAt(product.getCreatedAt()).updatedAt(product.getUpdatedAt()).build();
    }

    private void audit(String action, String actorId, Product product, String changes) {
        auditLogRepository.save(AuditLog.builder().action(action).actorId(actorId).actorType("USER")
                .targetEntity("Product").targetId(product.getId().toString()).changes(changes)
                .correlationId(CorrelationIdContext.getCorrelationId()).build());
    }

    private String normalize(String value) {
        return value.trim().toUpperCase();
    }
}
