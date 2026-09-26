package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.OrderRequest;
import com.sahastra.backend.api.dto.OrderResponse;
import com.sahastra.backend.domain.entity.Cart;
import com.sahastra.backend.domain.entity.CartItem;
import com.sahastra.backend.domain.entity.Product;
import com.sahastra.backend.domain.entity.User;
import com.sahastra.backend.domain.repository.CartItemRepository;
import com.sahastra.backend.domain.repository.CartRepository;
import com.sahastra.backend.domain.repository.IdempotencyKeyRepository;
import com.sahastra.backend.domain.repository.OrderRepository;
import com.sahastra.backend.domain.repository.ProductRepository;
import com.sahastra.backend.domain.repository.UserRepository;
import com.sahastra.backend.exception.BusinessException;
import com.sahastra.backend.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderCreatesOrderAndDeductsStock() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("buyer@example.com").firstName("Buyer").lastName("User").passwordHash("hash").build();
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .sku("SKU-1")
                .name("Notebook")
                .description("Desk notebook")
                .basePrice(new BigDecimal("24.00"))
                .stock(5)
                .active(true)
                .build();

        Cart cart = Cart.builder()
                .id(UUID.randomUUID())
                .user(user)
                .items(new ArrayList<>())
                .active(true)
                .build();

        CartItem cartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("24.00"))
                .build();
        cart.getItems().add(cartItem);

        OrderRequest request = OrderRequest.builder()
                .shippingAddress("123 Main St")
                .city("Seattle")
                .state("WA")
                .postalCode("98101")
                .country("US")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(idempotencyKeyRepository.findByUserIdAndKey(userId, "idem-1")).thenReturn(Optional.empty());
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        OrderResponse response = orderService.createOrder(userId, "idem-1", request);

        assertEquals(userId, response.getUserId());
        assertEquals(new BigDecimal("48.00"), response.getSubtotal());
        assertEquals(3, product.getStock());
    }

    @Test
    void createOrderRejectsDuplicateIdempotencyKey() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("buyer@example.com").firstName("Buyer").lastName("User").passwordHash("hash").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(idempotencyKeyRepository.findByUserIdAndKey(userId, "idem-dup")).thenReturn(Optional.of(new Object()));

        OrderRequest request = OrderRequest.builder()
                .shippingAddress("123 Main St")
                .city("Seattle")
                .state("WA")
                .postalCode("98101")
                .country("US")
                .build();

        assertThrows(BusinessException.class,
                () -> orderService.createOrder(userId, "idem-dup", request));
    }
}