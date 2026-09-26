package com.sahastra.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahastra.backend.domain.document.ProductDocument;
import com.sahastra.backend.domain.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;

/**
 * Service for synchronizing products to OpenSearch index.
 * Handles document creation, updates, and deletions.
 */
@Service
@Slf4j
public class ProductIndexService {

    private static final String PRODUCTS_INDEX = "products";
    private final OpenSearchClient openSearchClient;
    private final ObjectMapper objectMapper;

    public ProductIndexService(OpenSearchClient openSearchClient, ObjectMapper objectMapper) {
        this.openSearchClient = openSearchClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Index a product document.
     */
    public void indexProduct(Product product) {
        try {
            ProductDocument doc = toDocument(product);
            IndexRequest.Builder<ProductDocument> builder = new IndexRequest.Builder<ProductDocument>()
                    .index(PRODUCTS_INDEX)
                    .id(product.getId().toString())
                    .document(doc);
            
            openSearchClient.index(builder.build());
            log.debug("Product indexed: {}", product.getId());
        } catch (Exception e) {
            log.error("Failed to index product: {}", product.getId(), e);
        }
    }

    /**
     * Remove a product document from index.
     */
    public void deleteProduct(String productId) {
        try {
            DeleteRequest.Builder builder = new DeleteRequest.Builder()
                    .index(PRODUCTS_INDEX)
                    .id(productId);
            
            openSearchClient.delete(builder.build());
            log.debug("Product deleted from index: {}", productId);
        } catch (Exception e) {
            log.error("Failed to delete product from index: {}", productId, e);
        }
    }

    private ProductDocument toDocument(Product product) {
        return ProductDocument.builder()
                .id(product.getId().toString())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategory().getId().toString())
                .categoryName(product.getCategory().getName())
                .categorySlug(product.getCategory().getSlug())
                .brandId(product.getBrand().getId().toString())
                .brandName(product.getBrand().getName())
                .brandSlug(product.getBrand().getSlug())
                .basePrice(product.getBasePrice())
                .effectivePrice(product.effectivePrice(Instant.now()))
                .discountType(product.getDiscountType() != null ? product.getDiscountType().name() : null)
                .discountValue(product.getDiscountValue())
                .discountStartsAt(product.getDiscountStartsAt())
                .discountEndsAt(product.getDiscountEndsAt())
                .stock(product.getStock())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .suggest(ProductDocument.SuggestField.builder()
                        .input(new String[]{product.getName(), product.getSku(), product.getBrand().getName()})
                        .weight(product.isActive() ? 10 : 1)
                        .build())
                .build();
    }
}
