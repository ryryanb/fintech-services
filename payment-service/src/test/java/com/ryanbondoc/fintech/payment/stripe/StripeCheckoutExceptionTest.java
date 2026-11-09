package com.ryanbondoc.fintech.payment.stripe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class StripeCheckoutExceptionTest {

    @Test
    void mapsAmountTooSmallToBadRequest() {
        StripeCheckoutException exception = new StripeCheckoutException(
                "Unable to create Stripe Checkout Session.",
                new RuntimeException("amount too small"),
                "amount_too_small");

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void mapsOtherStripeErrorsToBadGateway() {
        StripeCheckoutException exception = new StripeCheckoutException(
                "Unable to create Stripe Checkout Session.",
                new RuntimeException("Stripe unavailable"),
                "api_error");

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }

    @Test
    void defaultsToBadGatewayWhenStripeErrorCodeIsMissing() {
        StripeCheckoutException exception = new StripeCheckoutException(
                "Unable to create Stripe Checkout Session.",
                new RuntimeException("Stripe unavailable"));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }
}
