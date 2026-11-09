package com.ryanbondoc.fintech.payment.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Operation(
            summary = "Create a Stripe Checkout session",
            description = "Creates a pending payment for the authenticated customer and returns its Stripe-hosted Checkout URL.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Checkout session created"),
            @ApiResponse(responseCode = "400", description = "Invalid checkout request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "502", description = "Stripe Checkout session could not be created")
    })
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
