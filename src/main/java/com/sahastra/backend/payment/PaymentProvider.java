package com.sahastra.backend.payment;

import java.math.BigDecimal;

public interface PaymentProvider {
    PaymentResult charge(String paymentMethod, BigDecimal amount, String currency, String referenceId);

    PaymentResult refund(String paymentId, BigDecimal amount, String currency);

    String getName();
}
