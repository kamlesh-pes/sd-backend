package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.CartItemRequest;
import com.sahastra.backend.api.dto.CartItemResponse;
import com.sahastra.backend.api.dto.CartResponse;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.domain.entity.Cart;
import com.sahastra.backend.domain.entity.CartItem;
import com.sahastra.backend.domain.entity.Product;
import com.sahastra.backend.domain.entity.User;
import com.sahastra.backend.domain.repository.CartItemRepository;
import com.sahastra.backend.domain.repository.CartRepository;
import com.sahastra.backend.domain.repository.ProductRepository;
import com.sahastra.backend.domain.repository.UserRepository;
import com.sahastra.backend.exception.BusinessException;
import com.sahastra.backend.exception.ResourceNotFoundException;
import com.sahastra.backend.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                      CartItemRepository cartItemRepository,
                      ProductRepository productRepository,
                      UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponse getCart(UUID userId, String guestSessionId) {
        Cart cart = findOrCreateCart(userId, guestSessionId);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(UUID userId, String guestSessionId, UUID productId, int quantity) {
        if (quantity < 1) {
            throw new ValidationException("Quantity must be at least 1");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));

        if (!product.isActive()) {
            throw new BusinessException("PRODUCT_INACTIVE", "Product is not available");
        }

        if (quantity > product.getStock()) {
            throw new ValidationException("Requested quantity exceeds available stock");
        }

        Cart cart = findOrCreateCart(userId, guestSessionId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseGet(() -> CartItem.builder().cart(cart).product(product).unitPrice(product.effectivePrice(Instant.now())).build());

        int nextQuantity = item.getQuantity() + quantity;
        if (nextQuantity > product.getStock()) {
            throw new ValidationException("Cart quantity exceeds available stock");
        }

        item.setQuantity(nextQuantity);
        item.setUnitPrice(product.effectivePrice(Instant.now()));
        if (item.getId() == null) {
            cart.getItems().add(item);
        }

        cartRepository.save(cart);
        cartItemRepository.save(item);
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItem(UUID userId, String guestSessionId, UUID productId, int quantity) {
        if (quantity < 1) {
            throw new ValidationException("Quantity must be at least 1");
        }

        Cart cart = findOrCreateCart(userId, guestSessionId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", productId.toString()));

        Product product = item.getProduct();
        if (quantity > product.getStock()) {
            throw new ValidationException("Requested quantity exceeds available stock");
        }

        item.setQuantity(quantity);
        item.setUnitPrice(product.effectivePrice(Instant.now()));
        cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, String guestSessionId, UUID productId) {
        Cart cart = findOrCreateCart(userId, guestSessionId);
        Optional<CartItem> itemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);
        if (itemOpt.isEmpty()) {
            return toResponse(cart);
        }

        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartItemRepository.delete(itemOpt.get());
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse mergeGuestCart(UUID userId, String guestSessionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        Optional<Cart> guestCartOpt = cartRepository.findByGuestSessionId(guestSessionId);
        if (guestCartOpt.isEmpty()) {
            Optional<Cart> existing = cartRepository.findByUserId(userId);
            return existing.map(this::toResponse).orElseGet(() -> getCart(userId, null));
        }

        Cart guestCart = guestCartOpt.get();
        Cart userCart = cartRepository.findByUserId(userId).orElseGet(() -> Cart.builder().user(user).active(true).build());
        userCart.setUser(user);

        for (CartItem item : guestCart.getItems()) {
            Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(userCart.getId(), item.getProduct().getId());
            if (existingItem.isPresent()) {
                int mergedQty = existingItem.get().getQuantity() + item.getQuantity();
                if (mergedQty > item.getProduct().getStock()) {
                    mergedQty = item.getProduct().getStock();
                }
                existingItem.get().setQuantity(mergedQty);
                existingItem.get().setUnitPrice(item.getProduct().effectivePrice(Instant.now()));
                cartItemRepository.save(existingItem.get());
            } else {
                CartItem cloned = CartItem.builder()
                        .cart(userCart)
                        .product(item.getProduct())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getProduct().effectivePrice(Instant.now()))
                        .build();
                userCart.getItems().add(cloned);
                cartItemRepository.save(cloned);
            }
        }

        cartRepository.save(userCart);
        cartRepository.delete(guestCart);
        return toResponse(userCart);
    }

    @Transactional(readOnly = true)
    public void cleanupExpiredGuestCarts(Instant cutoff) {
        List<Cart> stale = cartRepository.findByUserIdIsNullAndUpdatedAtBefore(cutoff);
        for (Cart cart : stale) {
            cartRepository.delete(cart);
        }
    }

    private Cart findOrCreateCart(UUID userId, String guestSessionId) {
        if (userId != null) {
            return cartRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        Cart cart = Cart.builder().user(userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()))).guestSessionId(guestSessionId).active(true).build();
                        return cartRepository.save(cart);
                    });
        }

        if (guestSessionId != null && !guestSessionId.isBlank()) {
            return cartRepository.findByGuestSessionId(guestSessionId)
                    .orElseGet(() -> cartRepository.save(Cart.builder().guestSessionId(guestSessionId).active(true).build()));
        }

        throw new ValidationException("A cart requires either a user id or guest session id");
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            BigDecimal unitPrice = product.effectivePrice(Instant.now());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            items.add(CartItemResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .sku(product.getSku())
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .effectiveUnitPrice(unitPrice)
                    .build());
        }

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .guestSessionId(cart.getGuestSessionId())
                .items(items)
                .subtotal(subtotal)
                .total(subtotal)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
