package com.ryanbondoc.fintech.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
import com.ryanbondoc.fintech.payment.stripe.StripeCheckoutException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CheckoutGateway stripeCheckoutGateway;
    @Mock
    private CheckoutGateway paypalCheckoutGateway;
    @Mock
    private PayPalCheckoutGateway paypalGateway;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, stripeCheckoutGateway, paypalCheckoutGateway, paypalGateway);
    }

    @Test
    void initiateCheckoutPersistsPendingPaymentAndReturnsStripeUrl() {
        Payment payment = pendingPayment("customer-123", "25.00", "USD", "Demo purchase", PaymentProvider.STRIPE, "checkout-1");
        when(paymentRepository.findByCustomerIdAndIdempotencyKey("customer-123", "checkout-1"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeCheckoutGateway.createSession(any(Payment.class)))
                .thenReturn(new CheckoutSessionDetails("cs_test_123", "https://checkout.stripe.com/c/pay/cs_test_123"));

        CheckoutResponse response = paymentService.initiateCheckout(
                "customer-123", new CreateCheckoutRequest(new BigDecimal("25.00"), "usd", "Demo purchase"), "checkout-1");

        assertNotNull(response.paymentId());
        assertEquals(new BigDecimal("25.00"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("https://checkout.stripe.com/c/pay/cs_test_123", response.checkoutUrl());
        verify(paymentRepository).insertIfAbsent(any(UUID.class), any(String.class), any(BigDecimal.class),
                any(String.class), any(String.class), any(String.class), any(String.class), any(String.class));
        verify(paymentRepository).save(payment);
    }

    @Test
    void retryWithSameKeyAndSameRequestReturnsExistingCheckoutWithoutCallingProviderAgain() {
        Payment payment = pendingPayment("customer-123", "25.00", "USD", "Demo purchase", PaymentProvider.STRIPE, "checkout-1");
        payment.attachCheckoutDetails("cs_test_existing", "https://checkout.stripe.com/c/pay/cs_test_existing");
        when(paymentRepository.findByCustomerIdAndIdempotencyKey("customer-123", "checkout-1"))
                .thenReturn(Optional.of(payment));

        CheckoutResponse response = paymentService.initiateCheckout(
                "customer-123", new CreateCheckoutRequest(new BigDecimal("25.00"), "USD", "Demo purchase"), "checkout-1");

        assertEquals(payment.getId(), response.paymentId());
        assertEquals("https://checkout.stripe.com/c/pay/cs_test_existing", response.checkoutUrl());
        verify(stripeCheckoutGateway, never()).createSession(any(Payment.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void reusingKeyWithDifferentRequestReturnsConflict() {
        Payment payment = pendingPayment("customer-123", "25.00", "USD", "Demo purchase", PaymentProvider.STRIPE, "checkout-1");
        when(paymentRepository.findByCustomerIdAndIdempotencyKey("customer-123", "checkout-1"))
                .thenReturn(Optional.of(payment));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> paymentService.initiateCheckout("customer-123",
                        new CreateCheckoutRequest(new BigDecimal("30.00"), "USD", "Demo purchase"), "checkout-1"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(stripeCheckoutGateway, never()).createSession(any(Payment.class));
    }

    @Test
    void initiateCheckoutUsesPayPalGatewayWhenPayPalIsSelected() {
        Payment payment = pendingPayment("customer-123", "100.00", "PHP", "Demo purchase", PaymentProvider.PAYPAL, "paypal-1");
        when(paymentRepository.findByCustomerIdAndIdempotencyKey("customer-123", "paypal-1"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paypalCheckoutGateway.createSession(any(Payment.class)))
                .thenReturn(new CheckoutSessionDetails("PAYPAL-ORDER-123",
                        "https://www.sandbox.paypal.com/checkoutnow?token=PAYPAL-ORDER-123"));

        CheckoutResponse response = paymentService.initiateCheckout("customer-123",
                new CreateCheckoutRequest(new BigDecimal("100.00"), "PHP", "Demo purchase", "PAYPAL"), "paypal-1");

        assertEquals(PaymentProvider.PAYPAL, response.provider());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("https://www.sandbox.paypal.com/checkoutnow?token=PAYPAL-ORDER-123", response.checkoutUrl());
        assertEquals("PAYPAL-ORDER-123", payment.getPaypalOrderId());
        verify(paypalCheckoutGateway).createSession(payment);
    }

    @Test
    void leavesReservationPendingWhenProviderCallFailsSoRetryCanRecoverSameProviderOperation() {
        Payment payment = pendingPayment("customer-123", "25.00", "USD", "Demo purchase", PaymentProvider.STRIPE, "checkout-1");
        when(paymentRepository.findByCustomerIdAndIdempotencyKey("customer-123", "checkout-1"))
                .thenReturn(Optional.of(payment));
        when(stripeCheckoutGateway.createSession(payment))
                .thenThrow(new StripeCheckoutException("Stripe unavailable", new RuntimeException()));

        assertThrows(StripeCheckoutException.class, () -> paymentService.initiateCheckout(
                "customer-123", new CreateCheckoutRequest(new BigDecimal("25.00"), "USD", "Demo purchase"), "checkout-1"));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void capturePayPalPaymentMarksPaymentSucceededOnlyAfterMatchingCapture() {
        Payment payment = new Payment("customer-123", new BigDecimal("100.00"), "PHP",
                "Demo purchase", PaymentProvider.PAYPAL);
        UUID paymentId = payment.getId();
        payment.attachCheckoutDetails("PAYPAL-ORDER-123",
                "https://www.sandbox.paypal.com/checkoutnow?token=PAYPAL-ORDER-123");
        when(paymentRepository.findByIdAndCustomerId(paymentId, "customer-123")).thenReturn(Optional.of(payment));
        when(paypalGateway.captureOrder("PAYPAL-ORDER-123")).thenReturn(
                new PayPalCheckoutGateway.CapturedPayPalPayment("PAYPAL-ORDER-123", "CAPTURE-123", "100.00", "PHP"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutResponse response = paymentService.capturePayPalPayment(paymentId, "customer-123", "PAYPAL-ORDER-123");

        assertEquals(PaymentStatus.SUCCEEDED, response.status());
        verify(paypalGateway).captureOrder("PAYPAL-ORDER-123");
    }

    @Test
    void doesNotMarkPaymentFailedWhenSavingSuccessfulStripeSessionFails() {
        Payment payment = pendingPayment("customer-123", "25.00", "USD", "Demo purchase", PaymentProvider.STRIPE, "checkout-1");
        when(paymentRepository.findByCustomerIdAndIdempotencyKey("customer-123", "checkout-1"))
                .thenReturn(Optional.of(payment));
        when(stripeCheckoutGateway.createSession(payment))
                .thenReturn(new CheckoutSessionDetails("cs_test_456", "https://checkout.stripe.com/c/pay/cs_test_456"));
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new RuntimeException("database unavailable"));

        assertThrows(RuntimeException.class, () -> paymentService.initiateCheckout(
                "customer-123", new CreateCheckoutRequest(new BigDecimal("25.00"), "USD", "Demo purchase"), "checkout-1"));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals("cs_test_456", payment.getStripeSessionId());
    }


    @Test
    void stripeFailureWebhookMarksOnlyPaymentFailed() {
        Payment payment = pendingPayment("customer-123", "25.00", "USD", "Demo purchase", PaymentProvider.STRIPE, "checkout-failed");
        payment.attachCheckoutDetails("cs_test_failed", "https://checkout.stripe.com/c/pay/cs_test_failed");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.updateStripePaymentStatus(payment.getId(), "cs_test_failed", PaymentStatus.FAILED);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    void expiredStripeCheckoutMarksPaymentCancelled() {
        Payment payment = pendingPayment("customer-123", "25.00", "USD", "Demo purchase", PaymentProvider.STRIPE, "checkout-expired");
        payment.attachCheckoutDetails("cs_test_expired", "https://checkout.stripe.com/c/pay/cs_test_expired");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.updateStripePaymentStatus(payment.getId(), "cs_test_expired", PaymentStatus.CANCELLED);

        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    void duplicateOrOutOfOrderStripeEventsCannotOverwriteTerminalPaymentStatus() {
        Payment payment = pendingPayment("customer-123", "25.00", "USD", "Demo purchase", PaymentProvider.STRIPE, "checkout-terminal");
        payment.attachCheckoutDetails("cs_test_terminal", "https://checkout.stripe.com/c/pay/cs_test_terminal");
        payment.markSucceeded();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        paymentService.updateStripePaymentStatus(payment.getId(), "cs_test_terminal", PaymentStatus.FAILED);

        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void stripeEventWithMismatchedSessionCannotChangePaymentStatus() {
        Payment payment = pendingPayment("customer-123", "25.00", "USD", "Demo purchase", PaymentProvider.STRIPE, "checkout-mismatch");
        payment.attachCheckoutDetails("cs_test_expected", "https://checkout.stripe.com/c/pay/cs_test_expected");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> paymentService.updateStripePaymentStatus(payment.getId(), "cs_test_other", PaymentStatus.SUCCEEDED));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private Payment pendingPayment(String customerId, String amount, String currency, String description,
            PaymentProvider provider, String key) {
        Payment payment = new Payment(customerId, new BigDecimal(amount), currency, description, provider);
        payment.setIdempotency(key, fingerprint(new BigDecimal(amount), currency, description, provider));
        return payment;
    }

    private String fingerprint(BigDecimal amount, String currency, String description, PaymentProvider provider) {
        String canonical = amount.stripTrailingZeros().toPlainString() + "|" + currency + "|"
                + description + "|" + provider.name();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
