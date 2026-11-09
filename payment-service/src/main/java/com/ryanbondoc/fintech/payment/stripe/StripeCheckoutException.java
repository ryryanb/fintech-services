package com.ryanbondoc.fintech.payment.stripe;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class StripeCheckoutException extends ResponseStatusException {
    public StripeCheckoutException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public StripeCheckoutException(String message, Throwable cause, String stripeErrorCode) {
        super("amount_too_small".equals(stripeErrorCode) ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY,
                message, cause);
    }
}
