package com.ryanbondoc.fintech.payment.stripe;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class StripeCheckoutException extends RuntimeException {
    public StripeCheckoutException(String message, Throwable cause) {
        super(message, cause);
    }
}