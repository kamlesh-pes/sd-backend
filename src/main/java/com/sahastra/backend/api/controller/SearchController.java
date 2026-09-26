package com.sahastra.backend.api.controller;

import com.sahastra.backend.api.dto.ApiResponse;
import com.sahastra.backend.api.dto.SearchProductResponse;
import com.sahastra.backend.api.dto.SearchRequest;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for product search functionality.
 */
@RestController
@RequestMapping("/api/v1/search")
@Slf4j
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Search products with full-text query, filters, and sorting.
     * GET /api/v1/search?query=...&category=...&brand=...&minPrice=...&maxPrice=...&sortBy=...&page=...&size=...
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SearchProductResponse>>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Search request: query={}, category={}, brand={}, sortBy={}, page={}, size={}", 
                 query, category, brand, sortBy, page, size);
        
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .category(category)
                .brand(brand)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();
        
        Page<SearchProductResponse> results = searchService.search(searchRequest);
        
        return ResponseEntity.ok(ApiResponse.<Page<SearchProductResponse>>builder()
                .success(true)
                .data(results)
                .message("Search completed")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }

    /**
     * Type-ahead suggestions for product search.
     * GET /api/v1/search/suggest?q=...&limit=...
     */
    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<String>>> suggest(
            @RequestParam(name = "q") String prefix,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Suggestions request: prefix={}, limit={}", prefix, limit);
        
        List<String> suggestions = searchService.suggestions(prefix, limit);
        
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .success(true)
                .data(suggestions)
                .message("Suggestions retrieved")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }
}
