package com.sahastra.backend.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public PaymentResult charge(String paymentMethod, BigDecimal amount, String currency, String referenceId) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return PaymentResult.builder()
                    .success(false)
                    .status("FAILED")
                    .message("Payment method is required")
                    .build();
        }

        return PaymentResult.builder()
                .success(true)
                .paymentId("mock_payment_" + UUID.randomUUID())
                .status("SUCCEEDED")
                .message("Mock payment succeeded")
                .build();
    }

    @Override
    public PaymentResult refund(String paymentId, BigDecimal amount, String currency) {
        return PaymentResult.builder()
                .success(true)
                .paymentId(paymentId)
                .status("REFUNDED")
                .message("Mock refund processed")
                .build();
    }

    @Override
    public String getName() {
        return "mock";
    }
}
