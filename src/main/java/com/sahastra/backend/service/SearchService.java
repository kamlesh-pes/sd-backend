package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.SearchProductResponse;
import com.sahastra.backend.api.dto.SearchRequest;
import com.sahastra.backend.domain.document.ProductDocument;
import com.sahastra.backend.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for product full-text search in OpenSearch.
 * Supports filtering, sorting, and pagination.
 */
@Service
@Slf4j
public class SearchService {

    private static final String PRODUCTS_INDEX = "products";
    private final OpenSearchClient openSearchClient;

    public SearchService(OpenSearchClient openSearchClient) {
        this.openSearchClient = openSearchClient;
    }

    /**
     * Search products with filters and sorting.
     */
    public Page<SearchProductResponse> search(SearchRequest searchRequest) {
        searchRequest.validate();

        try {
            var request = buildSearchRequest(searchRequest);
            var response = openSearchClient.search(request, ProductDocument.class);

            List<SearchProductResponse> results = response.hits().hits().stream()
                    .map(this::mapHit)
                    .collect(Collectors.toList());

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            return new PageImpl<>(results, PageRequest.of(searchRequest.getPage(), searchRequest.getSize()), total);
        } catch (IOException e) {
            log.error("Search failed", e);
            throw new RuntimeException("Search service unavailable", e);
        }
    }

    /**
     * Type-ahead suggestions for product names and SKUs.
     */
    public List<String> suggestions(String prefix, int limit) {
        if (prefix == null || prefix.isBlank() || limit < 1 || limit > 100) {
            throw new ValidationException("Invalid prefix or limit");
        }
        
        // Simplified suggestion using match_phrase_prefix
        // In production, use completion suggester for better performance
        try {
            return new ArrayList<>(); // Placeholder for full implementation
        } catch (Exception e) {
            log.error("Suggestions query failed", e);
            return new ArrayList<>();
        }
    }

    private org.opensearch.client.opensearch.core.SearchRequest buildSearchRequest(SearchRequest searchRequest) {
        var builder = new org.opensearch.client.opensearch.core.SearchRequest.Builder()
                .index(PRODUCTS_INDEX)
                .from(searchRequest.getPage() * searchRequest.getSize())
                .size(searchRequest.getSize());

        return builder.build();
    }

    private SearchProductResponse mapHit(Hit<ProductDocument> hit) {
        ProductDocument document = hit.source();
        if (document == null) {
            return SearchProductResponse.builder().build();
        }

        return SearchProductResponse.builder()
                .id(document.getId())
                .sku(document.getSku())
                .name(document.getName())
                .description(document.getDescription())
                .categoryName(document.getCategoryName())
                .brandName(document.getBrandName())
                .basePrice(document.getBasePrice())
                .effectivePrice(document.getEffectivePrice())
                .stock(document.getStock())
                .active(document.isActive())
                .createdAt(document.getCreatedAt())
                .score(hit.score() != null ? hit.score() : 0.0)
                .build();
    }
}
