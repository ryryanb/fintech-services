package com.ryanbondoc.fintech.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ryanbondoc.fintech.payment.api.CheckoutResponse;
import com.ryanbondoc.fintech.payment.api.CreateCheckoutRequest;
import com.ryanbondoc.fintech.payment.domain.Payment;
import com.ryanbondoc.fintech.payment.domain.PaymentStatus;
import com.ryanbondoc.fintech.payment.domain.PaymentProvider;
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

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, stripeCheckoutGateway, paypalCheckoutGateway);
    }

    @Test
    void initiateCheckoutPersistsPendingPaymentAndReturnsStripeUrl() {
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeCheckoutGateway.createSession(any(Payment.class)))
                .thenReturn(new CheckoutSessionDetails("cs_test_123", "https://checkout.stripe.com/c/pay/cs_test_123"));

        CheckoutResponse response = paymentService.initiateCheckout(
                "customer-123",
                new CreateCheckoutRequest(new BigDecimal("25.00"), "usd", "Demo purchase"));

        assertNotNull(response.paymentId());
        assertEquals(new BigDecimal("25.00"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("https://checkout.stripe.com/c/pay/cs_test_123", response.checkoutUrl());
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void initiateCheckoutMarksPaymentFailedWhenStripeRejectsSessionCreation() {
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(checkoutGateway.createSession(any(Payment.class)))
                .thenThrow(new StripeCheckoutException("Stripe unavailable", new RuntimeException()));

        assertThrows(StripeCheckoutException.class, () -> paymentService.initiateCheckout(
                "customer-123",
                new CreateCheckoutRequest(new BigDecimal("25.00"), "USD", "Demo purchase")));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        assertEquals(PaymentStatus.FAILED, captor.getAllValues().get(1).getStatus());
    }

    @Test
    void initiateCheckoutUsesPayPalGatewayWhenPayPalIsSelected() {
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paypalCheckoutGateway.createSession(any(Payment.class)))
                .thenReturn(new CheckoutSessionDetails("PAYPAL-ORDER-123", "https://www.sandbox.paypal.com/checkoutnow?token=PAYPAL-ORDER-123"));

        CheckoutResponse response = paymentService.initiateCheckout(
                "customer-123",
                new CreateCheckoutRequest(new BigDecimal("100.00"), "PHP", "Demo purchase", "PAYPAL"));

        assertEquals(PaymentProvider.PAYPAL, response.provider());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("https://www.sandbox.paypal.com/checkoutnow?token=PAYPAL-ORDER-123", response.checkoutUrl());
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        assertEquals("PAYPAL-ORDER-123", captor.getAllValues().get(1).getPaypalOrderId());
        verify(paypalCheckoutGateway).createSession(any(Payment.class));
    }

    @Test
    void doesNotMarkPaymentFailedWhenSavingSuccessfulStripeSessionFails() {
        when(checkoutGateway.createSession(any(Payment.class)))
                .thenReturn(new CheckoutSessionDetails("cs_test_456", "https://checkout.stripe.com/c/pay/cs_test_456"));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new RuntimeException("database unavailable"));

        assertThrows(RuntimeException.class, () -> paymentService.initiateCheckout(
                "customer-123",
                new CreateCheckoutRequest(new BigDecimal("25.00"), "USD", "Demo purchase")));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        Payment paymentAfterStripeSuccess = captor.getAllValues().get(1);
        assertEquals(PaymentStatus.PENDING, paymentAfterStripeSuccess.getStatus());
        assertEquals("cs_test_456", paymentAfterStripeSuccess.getStripeSessionId());
    }
}
