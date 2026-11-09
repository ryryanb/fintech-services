package com.ryanbondoc.fintech.payment.stripe;

public class StripeCheckoutException extends RuntimeException {
    public StripeCheckoutException(String message, Throwable cause) {
        super(message, cause);
    }
}