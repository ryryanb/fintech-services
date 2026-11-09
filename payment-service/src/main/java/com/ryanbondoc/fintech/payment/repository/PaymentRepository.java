package com.ryanbondoc.fintech.payment.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ryanbondoc.fintech.payment.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdAndCustomerId(UUID id, String customerId);
    Optional<Payment> findByPaypalOrderId(String paypalOrderId);
    Optional<Payment> findByStripeSessionId(String stripeSessionId);
}