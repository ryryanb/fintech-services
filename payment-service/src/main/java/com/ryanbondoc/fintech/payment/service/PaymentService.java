package com.ryanbondoc.fintech.payment.service;

import org.springframework.stereotype.Service;

import com.ryanbondoc.fintech.payment.api.CheckoutResponse;
import com.ryanbondoc.fintech.payment.api.CreateCheckoutRequest;
import com.ryanbondoc.fintech.payment.domain.Payment;
import com.ryanbondoc.fintech.payment.repository.PaymentRepository;
import com.ryanbondoc.fintech.payment.stripe.CheckoutGateway;
import com.ryanbondoc.fintech.payment.stripe.CheckoutSessionDetails;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final CheckoutGateway checkoutGateway;

    public PaymentService(PaymentRepository paymentRepository, CheckoutGateway checkoutGateway) {
        this.paymentRepository = paymentRepository;
        this.checkoutGateway = checkoutGateway;
    }

    public CheckoutResponse initiateCheckout(String customerId, CreateCheckoutRequest request) {
        Payment payment = paymentRepository.save(new Payment(
                customerId,
                request.amount(),
                request.normalizedCurrency(),
                request.description().trim()));

        try {
            CheckoutSessionDetails session = checkoutGateway.createSession(payment);
            payment.attachCheckoutSession(session.sessionId(), session.checkoutUrl());
            payment = paymentRepository.save(payment);
            return new CheckoutResponse(
                    payment.getId(), payment.getAmount(), payment.getCurrency(),
                    payment.getDescription(), payment.getStatus(), payment.getCheckoutUrl(), payment.getCreatedAt());
        } catch (RuntimeException exception) {
            payment.markFailed();
            paymentRepository.save(payment);
            throw exception;
        }
    }
}