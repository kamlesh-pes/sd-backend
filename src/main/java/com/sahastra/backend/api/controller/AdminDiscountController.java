package com.sahastra.backend.api.controller;

import com.sahastra.backend.api.dto.ApiResponse;
import com.sahastra.backend.api.dto.DiscountRequest;
import com.sahastra.backend.api.dto.DiscountResponse;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.service.DiscountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products/{productId}/discount")
public class AdminDiscountController {
    private final DiscountService discountService;

    public AdminDiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DiscountResponse>> create(@PathVariable UUID productId,
                                                                 @Valid @RequestBody DiscountRequest request,
                                                                 Authentication authentication) {
        return response(HttpStatus.CREATED, "Discount created",
                discountService.create(productId, request, authentication.getName()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<DiscountResponse>> update(@PathVariable UUID productId,
                                                                 @Valid @RequestBody DiscountRequest request,
                                                                 Authentication authentication) {
        return response(HttpStatus.OK, "Discount updated",
                discountService.update(productId, request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DiscountResponse>> get(@PathVariable UUID productId) {
        return response(HttpStatus.OK, "Discount retrieved", discountService.get(productId));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID productId, Authentication authentication) {
        discountService.delete(productId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Discount deleted")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }

    private ResponseEntity<ApiResponse<DiscountResponse>> response(HttpStatus status, String message, DiscountResponse data) {
        return ResponseEntity.status(status).body(ApiResponse.<DiscountResponse>builder()
                .success(true)
                .data(data)
                .message(message)
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }
}
