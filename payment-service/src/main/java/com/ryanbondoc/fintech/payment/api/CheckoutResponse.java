package com.ryanbondoc.fintech.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ryanbondoc.fintech.payment.domain.PaymentStatus;

public record CheckoutResponse(
        UUID paymentId,
        BigDecimal amount,
        String currency,
        String description,
        PaymentStatus status,
        String checkoutUrl,
        Instant createdAt) { }