package com.ryanbondoc.fintech.payment.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ryanbondoc.fintech.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{paymentId}/complete/paypal")
    @Operation(summary = "Capture an approved PayPal payment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CheckoutResponse> capturePayPalPayment(
            @PathVariable UUID paymentId,
            @RequestParam String token,
            JwtAuthenticationToken authentication) {
        String customerId = customerId(authentication);
        return ResponseEntity.ok(paymentService.capturePayPalPayment(paymentId, customerId, token));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment status", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CheckoutResponse> getPayment(
            @PathVariable UUID paymentId, JwtAuthenticationToken authentication) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId, customerId(authentication)));
    }

    @Operation(
            summary = "Create a payment checkout",
            description = "Creates or retrieves a checkout using a customer-scoped Idempotency-Key. Reusing a key with different request details is rejected.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Checkout session created or replayed"),
            @ApiResponse(responseCode = "400", description = "Invalid checkout request or missing idempotency key"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Idempotency key reused with different request details"),
            @ApiResponse(responseCode = "502", description = "The payment provider could not create checkout")
    })
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(
            @Valid @RequestBody CreateCheckoutRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            JwtAuthenticationToken authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                paymentService.initiateCheckout(customerId(authentication), request, idempotencyKey));
    }

    private String customerId(JwtAuthenticationToken authentication) {
        String customerId = authentication.getToken().getClaimAsString("customerId");
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalStateException("Authenticated token does not contain a customerId claim.");
        }
        return customerId;
    }
}