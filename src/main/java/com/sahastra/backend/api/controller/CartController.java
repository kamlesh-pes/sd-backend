package com.sahastra.backend.api.controller;

import com.sahastra.backend.api.dto.ApiResponse;
import com.sahastra.backend.api.dto.CartItemRequest;
import com.sahastra.backend.api.dto.CartResponse;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.service.CartService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@Slf4j
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Session", required = false) String guestSessionId) {
        UUID userId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .data(cartService.getCart(userId, guestSessionId))
                .message("Cart retrieved")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Session", required = false) String guestSessionId,
            @Valid @RequestBody CartItemRequest request) {
        UUID userId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        CartResponse cartResponse = cartService.addItem(userId, guestSessionId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .data(cartResponse)
                .message("Item added to cart")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Session", required = false) String guestSessionId,
            @PathVariable UUID productId,
            @RequestBody CartItemRequest request) {
        UUID userId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        CartResponse cartResponse = cartService.updateItem(userId, guestSessionId, productId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .data(cartResponse)
                .message("Cart item updated")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Session", required = false) String guestSessionId,
            @PathVariable UUID productId) {
        UUID userId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        CartResponse cartResponse = cartService.removeItem(userId, guestSessionId, productId);
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .data(cartResponse)
                .message("Cart item removed")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }
}
