package com.ryanbondoc.fintech.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 120)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "stripe_session_id", length = 255, unique = true)
    private String stripeSessionId;

    @Column(name = "paypal_order_id", length = 255, unique = true)
    private String paypalOrderId;

    @Column(name = "checkout_url", length = 2048)
    private String checkoutUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() { }

    public Payment(String customerId, BigDecimal amount, String currency, String description) {
        this(customerId, amount, currency, description, PaymentProvider.STRIPE);
    }

    public Payment(String customerId, BigDecimal amount, String currency, String description, PaymentProvider provider) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency.toUpperCase();
        this.description = description;
        this.status = PaymentStatus.PENDING;
        this.provider = provider;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void attachCheckoutDetails(String providerReference, String url) {
        if (provider == PaymentProvider.STRIPE) {
            this.stripeSessionId = providerReference;
        } else if (provider == PaymentProvider.PAYPAL) {
            this.paypalOrderId = providerReference;
        }
        this.checkoutUrl = url;
    }

    public void attachCheckoutSession(String sessionId, String url) {
        this.provider = PaymentProvider.STRIPE;
        attachCheckoutDetails(sessionId, url);
    }

    public void markSucceeded() {
        if (this.status == PaymentStatus.PENDING) this.status = PaymentStatus.SUCCEEDED;
    }

    public void markFailed() {
        if (this.status == PaymentStatus.PENDING) this.status = PaymentStatus.FAILED;
    }

    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public PaymentStatus getStatus() { return status; }
    public PaymentProvider getProvider() { return provider; }
    public String getStripeSessionId() { return stripeSessionId; }
    public String getPaypalOrderId() { return paypalOrderId; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}