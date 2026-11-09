package com.ryanbondoc.fintech.payment.paypal;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PayPalCheckoutException extends ResponseStatusException {
    public PayPalCheckoutException(String reason, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, reason, cause);
    }
}
