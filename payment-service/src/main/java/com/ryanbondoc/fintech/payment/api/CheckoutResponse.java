package com.ryanbondoc.fintech.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ryanbondoc.fintech.payment.domain.PaymentStatus;
import com.ryanbondoc.fintech.payment.domain.PaymentProvider;

public record CheckoutResponse(
        UUID paymentId,
        BigDecimal amount,
        String currency,
        String description,
        PaymentProvider provider,
        PaymentStatus status,
        String checkoutUrl,
        Instant createdAt) { }