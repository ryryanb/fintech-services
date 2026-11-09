package com.ryanbondoc.fintech.payment.stripe;

public record CheckoutSessionDetails(String sessionId, String checkoutUrl) { }