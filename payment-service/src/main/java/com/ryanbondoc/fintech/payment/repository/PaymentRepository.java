package com.ryanbondoc.fintech.payment.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ryanbondoc.fintech.payment.domain.Payment;
import com.ryanbondoc.fintech.payment.domain.PaymentProvider;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdAndCustomerId(UUID id, String customerId);
    Optional<Payment> findByCustomerIdAndIdempotencyKey(String customerId, String idempotencyKey);
    Optional<Payment> findByPaypalOrderId(String paypalOrderId);
    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO payments (
                id, customer_id, amount, currency, description, status, provider,
                idempotency_key, request_fingerprint, created_at, updated_at
            )
            VALUES (
                :id, :customerId, :amount, :currency, :description, 'PENDING', :provider,
                :idempotencyKey, :fingerprint, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (customer_id, idempotency_key)
                WHERE idempotency_key IS NOT NULL
            DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("customerId") String customerId,
            @Param("amount") BigDecimal amount,
            @Param("currency") String currency,
            @Param("description") String description,
            @Param("provider") String provider,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("fingerprint") String fingerprint);
}