package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.OrderRequest;
import com.sahastra.backend.api.dto.OrderResponse;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.domain.entity.Cart;
import com.sahastra.backend.domain.entity.CartItem;
import com.sahastra.backend.domain.entity.IdempotencyKey;
import com.sahastra.backend.domain.entity.Order;
import com.sahastra.backend.domain.entity.OrderItem;
import com.sahastra.backend.domain.entity.OrderStatusHistory;
import com.sahastra.backend.domain.entity.Product;
import com.sahastra.backend.domain.entity.SupportRequest;
import com.sahastra.backend.domain.entity.User;
import com.sahastra.backend.domain.enums.OrderStatus;
import com.sahastra.backend.domain.enums.SupportRequestStatus;
import com.sahastra.backend.domain.repository.CartItemRepository;
import com.sahastra.backend.domain.repository.CartRepository;
import com.sahastra.backend.domain.repository.IdempotencyKeyRepository;
import com.sahastra.backend.domain.repository.OrderRepository;
import com.sahastra.backend.domain.repository.OrderStatusHistoryRepository;
import com.sahastra.backend.domain.repository.ProductRepository;
import com.sahastra.backend.domain.repository.SupportRequestRepository;
import com.sahastra.backend.domain.repository.UserRepository;
import com.sahastra.backend.exception.BusinessException;
import com.sahastra.backend.exception.ResourceNotFoundException;
import com.sahastra.backend.exception.ValidationException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class OrderService {
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");
    private static final BigDecimal SHIPPING_RATE = new BigDecimal("9.99");

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final SupportRequestRepository supportRequestRepository;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        IdempotencyKeyRepository idempotencyKeyRepository,
                        OrderStatusHistoryRepository orderStatusHistoryRepository,
                        SupportRequestRepository supportRequestRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.supportRequestRepository = supportRequestRepository;
    }

    @Transactional
    public Order cancelOrder(UUID userId, UUID orderId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Cancellation reason is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("ORDER_ACCESS_DENIED", "User does not own this order");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("ORDER_ALREADY_CANCELLED", "Order is already cancelled");
        }

        if (!canSelfCancel(order)) {
            requestSupport(userId, orderId, reason);
            throw new BusinessException("CANCELLATION_REQUIRES_SUPPORT_REVIEW",
                    "This order cannot be self-cancelled. A support request has been created for review.");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.getProductId().toString()));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(previousStatus)
                .toStatus(OrderStatus.CANCELLED)
                .actorId(userId.toString())
                .actorType("CUSTOMER")
                .reason(reason)
                .build());

        return order;
    }

    @Transactional
    public SupportRequest requestSupport(UUID userId, UUID orderId, String message) {
        if (message == null || message.isBlank()) {
            throw new ValidationException("Support message is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("ORDER_ACCESS_DENIED", "User does not own this order");
        }

        return supportRequestRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseGet(() -> supportRequestRepository.save(SupportRequest.builder()
                        .order(order)
                        .user(order.getUser())
                        .message(message)
                        .status(SupportRequestStatus.OPEN)
                        .build()));
    }

    private boolean canSelfCancel(Order order) {
        if (order.getStatus() == null || order.getStatus() == OrderStatus.CANCELLED) {
            return false;
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.OUT_FOR_DELIVERY || order.getStatus() == OrderStatus.DELIVERED) {
            return false;
        }

        Instant cutoff = Instant.now().minusSeconds(24L * 60L * 60L);
        return order.getCreatedAt() != null && !order.getCreatedAt().isBefore(cutoff);
    }

    @Transactional
    public OrderResponse createOrder(UUID userId, String idempotencyKey, OrderRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("Idempotency key is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        idempotencyKeyRepository.findByUserIdAndKey(userId, idempotencyKey)
                .ifPresent(existing -> {
                    throw new BusinessException("IDEMPOTENT_REQUEST", "Duplicate order submission for the same idempotency key");
                });

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", userId.toString()));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ValidationException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", cartItem.getProduct().getId().toString()));

            if (!product.isActive()) {
                throw new BusinessException("PRODUCT_INACTIVE", "Product is not available: " + product.getName());
            }
            if (product.getStock() < cartItem.getQuantity()) {
                throw new BusinessException("INSUFFICIENT_STOCK", "Insufficient stock for product: " + product.getName());
            }

            BigDecimal unitPrice = product.effectivePrice(Instant.now());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem item = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .sku(product.getSku())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .build();
            orderItems.add(item);

            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
        }

        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal shipping = cart.getItems().size() > 0 ? SHIPPING_RATE : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(tax).add(shipping).setScale(2, java.math.RoundingMode.HALF_UP);

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal.setScale(2, java.math.RoundingMode.HALF_UP))
                .tax(tax)
                .shipping(shipping)
                .total(total)
                .shippingAddress(request.getShippingAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .idempotencyKey(idempotencyKey)
                .build();

        Order savedOrder = orderRepository.save(order);
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            savedOrder.getItems().add(item);
        }
        orderRepository.save(savedOrder);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(savedOrder)
                .fromStatus(null)
                .toStatus(OrderStatus.PENDING)
                .actorId(userId.toString())
                .actorType("USER")
                .reason("Order created")
                .build());

        IdempotencyKey idem = IdempotencyKey.builder()
                .user(user)
                .key(idempotencyKey)
                .order(savedOrder)
                .build();
        idempotencyKeyRepository.save(idem);

        cart.getItems().clear();
        cartRepository.save(cart);
        cartItemRepository.deleteByCartId(cart.getId());

        return toResponse(savedOrder);
    }

    private OrderResponse toResponse(Order order) {
        List<com.sahastra.backend.api.dto.OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemResponses.add(com.sahastra.backend.api.dto.OrderItemResponse.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .sku(item.getSku())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .lineTotal(item.getLineTotal())
                    .build());
        }

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .shipping(order.getShipping())
                .total(order.getTotal())
                .shippingAddress(order.getShippingAddress())
                .city(order.getCity())
                .state(order.getState())
                .postalCode(order.getPostalCode())
                .country(order.getCountry())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
