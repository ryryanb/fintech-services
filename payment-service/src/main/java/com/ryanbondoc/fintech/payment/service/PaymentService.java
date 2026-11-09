package com.ryanbondoc.fintech.payment.service;

import org.springframework.stereotype.Service;

import com.ryanbondoc.fintech.payment.api.CheckoutResponse;
import com.ryanbondoc.fintech.payment.api.CreateCheckoutRequest;
import com.ryanbondoc.fintech.payment.domain.Payment;
import com.ryanbondoc.fintech.payment.domain.PaymentProvider;
import com.ryanbondoc.fintech.payment.repository.PaymentRepository;
import com.ryanbondoc.fintech.payment.stripe.CheckoutGateway;
import com.ryanbondoc.fintech.payment.stripe.CheckoutSessionDetails;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final CheckoutGateway stripeCheckoutGateway;
    private final CheckoutGateway paypalCheckoutGateway;

    public PaymentService(
            PaymentRepository paymentRepository,
            @org.springframework.beans.factory.annotation.Qualifier("stripeCheckoutGateway") CheckoutGateway stripeCheckoutGateway,
            @org.springframework.beans.factory.annotation.Qualifier("paypalCheckoutGateway") CheckoutGateway paypalCheckoutGateway) {
        this.paymentRepository = paymentRepository;
        this.stripeCheckoutGateway = stripeCheckoutGateway;
        this.paypalCheckoutGateway = paypalCheckoutGateway;
    }

    public CheckoutResponse initiateCheckout(String customerId, CreateCheckoutRequest request) {
        Payment payment = paymentRepository.save(new Payment(
                customerId,
                request.amount(),
                request.normalizedCurrency(),
                request.description().trim(), request.normalizedProvider()));

        final CheckoutSessionDetails session;
        try {
            session = gatewayFor(payment.getProvider()).createSession(payment);
        } catch (RuntimeException exception) {
            payment.markFailed();
            paymentRepository.save(payment);
            throw exception;
        }

        // Persisting the Stripe response is deliberately outside the Stripe failure
        // handler. Stripe may already have created a payable session; a database
        // failure here must not incorrectly mark that payment as FAILED.
        payment.attachCheckoutDetails(session.sessionId(), session.checkoutUrl());
        payment = paymentRepository.save(payment);

        return new CheckoutResponse(
                payment.getId(), payment.getAmount(), payment.getCurrency(),
                payment.getDescription(), payment.getProvider(), payment.getStatus(), payment.getCheckoutUrl(), payment.getCreatedAt());
    }
}
