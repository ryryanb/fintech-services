package com.ryanbondoc.fintech.payment.stripe;

import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ryanbondoc.fintech.payment.domain.Payment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Component
public class StripeCheckoutGateway implements CheckoutGateway {
    private final String secretKey;
    private final String frontendBaseUrl;

    public StripeCheckoutGateway(
            @Value("${stripe.secret-key:}") String secretKey,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.secretKey = secretKey;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public CheckoutSessionDetails createSession(Payment payment) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Stripe is not configured. Set STRIPE_SECRET_KEY to a Stripe test secret key.");
        }

        Stripe.apiKey = secretKey;
        long amountInMinorUnits = payment.getAmount()
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendBaseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendBaseUrl + "/payment/cancel?payment_id=" + payment.getId())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(payment.getCurrency().toLowerCase())
                                                .setUnitAmount(amountInMinorUnits)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(payment.getDescription())
                                                                .build())
                                                .build())
                                .build())
                .putMetadata("paymentId", payment.getId().toString())
                .putMetadata("customerId", payment.getCustomerId())
                .build();

        try {
            Session session = Session.create(params);
            if (session.getUrl() == null || session.getUrl().isBlank()) {
                throw new IllegalStateException("Stripe did not return a Checkout URL.");
            }
            return new CheckoutSessionDetails(session.getId(), session.getUrl());
        } catch (StripeException exception) {
            throw new StripeCheckoutException("Unable to create Stripe Checkout Session.", exception);
        }
    }
}