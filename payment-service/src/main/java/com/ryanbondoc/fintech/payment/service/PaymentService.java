package com.ryanbondoc.fintech.payment.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ryanbondoc.fintech.payment.api.CheckoutResponse;
import com.ryanbondoc.fintech.payment.api.CreateCheckoutRequest;
import com.ryanbondoc.fintech.payment.domain.Payment;
import com.ryanbondoc.fintech.payment.domain.PaymentProvider;
import com.ryanbondoc.fintech.payment.domain.PaymentStatus;
import com.ryanbondoc.fintech.payment.paypal.PayPalCheckoutGateway;
import com.ryanbondoc.fintech.payment.repository.PaymentRepository;
import com.ryanbondoc.fintech.payment.stripe.CheckoutGateway;
import com.ryanbondoc.fintech.payment.stripe.CheckoutSessionDetails;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final CheckoutGateway stripeCheckoutGateway;
    private final CheckoutGateway paypalCheckoutGateway;
    private final PayPalCheckoutGateway paypalGateway;

    public PaymentService(
            PaymentRepository paymentRepository,
            @org.springframework.beans.factory.annotation.Qualifier("stripeCheckoutGateway") CheckoutGateway stripeCheckoutGateway,
            @org.springframework.beans.factory.annotation.Qualifier("paypalCheckoutGateway") CheckoutGateway paypalCheckoutGateway,
            PayPalCheckoutGateway paypalGateway) {
        this.paymentRepository = paymentRepository;
        this.stripeCheckoutGateway = stripeCheckoutGateway;
        this.paypalCheckoutGateway = paypalCheckoutGateway;
        this.paypalGateway = paypalGateway;
    }

    private CheckoutGateway gatewayFor(PaymentProvider provider) {
        return switch (provider) {
            case STRIPE -> stripeCheckoutGateway;
            case PAYPAL -> paypalCheckoutGateway;
        };
    }

    @Transactional
    public CheckoutResponse capturePayPalPayment(UUID paymentId, String customerId, String approvedOrderId) {
        Payment payment = paymentRepository.findByIdAndCustomerId(paymentId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found."));
        if (payment.getProvider() != PaymentProvider.PAYPAL || payment.getPaypalOrderId() == null
                || !payment.getPaypalOrderId().equals(approvedOrderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PayPal order does not match this payment.");
        }
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) return toResponse(payment);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is not pending.");
        }
        PayPalCheckoutGateway.CapturedPayPalPayment capture = paypalGateway.captureOrder(approvedOrderId);
        boolean matches = approvedOrderId.equals(capture.orderId())
                && payment.getAmount().compareTo(new BigDecimal(capture.amount())) == 0
                && payment.getCurrency().equalsIgnoreCase(capture.currency())
                && capture.captureId() != null && !capture.captureId().isBlank();
        if (!matches) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "PayPal capture details did not match the original payment.");
        payment.markSucceeded();
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public void markStripePaymentSucceeded(UUID paymentId, String stripeSessionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found."));
        if (payment.getProvider() != PaymentProvider.STRIPE || !stripeSessionId.equals(payment.getStripeSessionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe session does not match this payment.");
        }
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.markSucceeded();
            paymentRepository.save(payment);
        }
    }

    @Transactional(readOnly = true)
    public CheckoutResponse getPayment(UUID paymentId, String customerId) {
        Payment payment = paymentRepository.findByIdAndCustomerId(paymentId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found."));
        return toResponse(payment);
    }

    private CheckoutResponse toResponse(Payment payment) {
        return new CheckoutResponse(payment.getId(), payment.getAmount(), payment.getCurrency(),
                payment.getDescription(), payment.getProvider(), payment.getStatus(),
                payment.getCheckoutUrl(), payment.getCreatedAt());
    }

    @Transactional
    public CheckoutResponse initiateCheckout(String customerId, CreateCheckoutRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must contain between 1 and 128 characters.");
        }

        String currency = request.normalizedCurrency();
        String description = request.description().trim();
        PaymentProvider provider = request.normalizedProvider();
        String fingerprint = fingerprint(request.amount(), currency, description, provider);

        Payment payment = paymentRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)
                .orElse(null);

        if (payment != null) {
            if (!fingerprint.equals(payment.getRequestFingerprint())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key has already been used with different payment details.");
            }
            if (payment.getCheckoutUrl() != null && !payment.getCheckoutUrl().isBlank()) {
                return toResponse(payment);
            }
            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "The original checkout request did not complete. Use a new Idempotency-Key to start a new payment.");
            }
        } else {
            payment = new Payment(customerId, request.amount(), currency, description, provider);
            payment.setIdempotency(idempotencyKey, fingerprint);
            payment = paymentRepository.saveAndFlush(payment);
        }

        final CheckoutSessionDetails session;
        try {
            // The stable payment UUID is the provider idempotency key. A retry after a
            // timeout or process restart therefore cannot create a second provider order.
            session = gatewayFor(payment.getProvider()).createSession(payment);
        } catch (RuntimeException exception) {
            payment.markFailed();
            paymentRepository.save(payment);
            throw exception;
        }

        payment.attachCheckoutDetails(session.sessionId(), session.checkoutUrl());
        payment = paymentRepository.save(payment);
        return toResponse(payment);
    }

    private String fingerprint(BigDecimal amount, String currency, String description, PaymentProvider provider) {
        String canonical = amount.stripTrailingZeros().toPlainString() + "|" + currency + "|"
                + description + "|" + provider.name();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}