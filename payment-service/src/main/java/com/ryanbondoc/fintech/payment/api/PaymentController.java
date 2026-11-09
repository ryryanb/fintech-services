package com.ryanbondoc.fintech.payment.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import com.ryanbondoc.fintech.payment.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(
            @Valid @RequestBody CreateCheckoutRequest request,
            JwtAuthenticationToken authentication) {
        String customerId = authentication.getToken().getClaimAsString("customerId");
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalStateException("Authenticated token does not contain a customerId claim.");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiateCheckout(customerId, request));
    }
}