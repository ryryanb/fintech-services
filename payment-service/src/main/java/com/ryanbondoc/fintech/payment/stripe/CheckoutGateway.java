package com.ryanbondoc.fintech.payment.stripe;

import com.ryanbondoc.fintech.payment.domain.Payment;

public interface CheckoutGateway {
    CheckoutSessionDetails createSession(Payment payment);
}