package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.PaymentRequest;
import com.sahastra.backend.api.dto.PaymentResponse;
import com.sahastra.backend.domain.entity.Order;
import com.sahastra.backend.domain.entity.OrderStatusHistory;
import com.sahastra.backend.domain.entity.User;
import com.sahastra.backend.domain.enums.OrderStatus;
import com.sahastra.backend.domain.repository.OrderRepository;
import com.sahastra.backend.domain.repository.OrderStatusHistoryRepository;
import com.sahastra.backend.exception.BusinessException;
import com.sahastra.backend.exception.ResourceNotFoundException;
import com.sahastra.backend.exception.ValidationException;
import com.sahastra.backend.payment.MockPaymentProvider;
import com.sahastra.backend.payment.PaymentProvider;
import com.sahastra.backend.payment.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentProvider paymentProvider;

    public PaymentService(OrderRepository orderRepository,
                          OrderStatusHistoryRepository orderStatusHistoryRepository,
                          MockPaymentProvider paymentProvider) {
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.paymentProvider = paymentProvider;
    }

    @Transactional
    public PaymentResponse processPayment(UUID userId, UUID orderId, PaymentRequest request) {
        if (request == null) {
            throw new ValidationException("Payment request is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("ORDER_ACCESS_DENIED", "User does not own this order");
        }

        if (request.getAmount().compareTo(order.getTotal()) != 0) {
            throw new BusinessException("PAYMENT_AMOUNT_MISMATCH", "Payment amount does not match order total");
        }

        PaymentResult result = paymentProvider.charge(request.getPaymentMethod(), request.getAmount(), request.getCurrency(), order.getId().toString());
        if (!result.isSuccess()) {
            throw new BusinessException("PAYMENT_FAILED", result.getMessage());
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(OrderStatus.PENDING)
                .toStatus(OrderStatus.CONFIRMED)
                .actorId("system")
                .actorType("SYSTEM")
                .reason("Payment succeeded")
                .build());

        return PaymentResponse.builder()
                .orderId(order.getId())
                .provider(paymentProvider.getName())
                .paymentId(result.getPaymentId())
                .status(result.getStatus())
                .amount(order.getTotal())
                .currency(request.getCurrency())
                .processedAt(Instant.now())
                .orderStatus(order.getStatus())
                .build();
    }
}
