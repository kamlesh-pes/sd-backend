package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.PaymentRequest;
import com.sahastra.backend.api.dto.PaymentResponse;
import com.sahastra.backend.domain.entity.Order;
import com.sahastra.backend.domain.entity.User;
import com.sahastra.backend.domain.enums.OrderStatus;
import com.sahastra.backend.domain.repository.OrderRepository;
import com.sahastra.backend.domain.repository.OrderStatusHistoryRepository;
import com.sahastra.backend.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPaymentMarksOrderConfirmedWhenProviderSucceeds() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        User user = User.builder().id(userId).email("buyer@example.com").firstName("Buyer").lastName("User").passwordHash("hash").build();
        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("50.00"))
                .tax(new BigDecimal("5.00"))
                .shipping(new BigDecimal("9.99"))
                .total(new BigDecimal("64.99"))
                .shippingAddress("123 Main St")
                .city("Seattle")
                .state("WA")
                .postalCode("98101")
                .country("US")
                .idempotencyKey("idem-42")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentRequest request = PaymentRequest.builder()
                .provider("mock")
                .paymentMethod("card")
                .amount(new BigDecimal("64.99"))
                .currency("USD")
                .build();

        PaymentResponse response = paymentService.processPayment(userId, orderId, request);

        assertEquals("mock", response.getProvider());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals("SUCCEEDED", response.getStatus());
    }

    @Test
    void processPaymentRejectsMismatchedAmount() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        User user = User.builder().id(userId).email("buyer@example.com").firstName("Buyer").lastName("User").passwordHash("hash").build();
        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("50.00"))
                .tax(new BigDecimal("5.00"))
                .shipping(new BigDecimal("9.99"))
                .total(new BigDecimal("64.99"))
                .shippingAddress("123 Main St")
                .city("Seattle")
                .state("WA")
                .postalCode("98101")
                .country("US")
                .idempotencyKey("idem-43")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentRequest request = PaymentRequest.builder()
                .provider("mock")
                .paymentMethod("card")
                .amount(new BigDecimal("10.00"))
                .currency("USD")
                .build();

        assertThrows(BusinessException.class,
                () -> paymentService.processPayment(userId, orderId, request));
    }
}
