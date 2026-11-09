package com.ryanbondoc.fintech.payment.stripe;

import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ryanbondoc.fintech.payment.domain.Payment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.StripeError;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;

@Component
public class StripeCheckoutGateway implements CheckoutGateway {
    private static final Logger log = LoggerFactory.getLogger(StripeCheckoutGateway.class);

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
                .setSuccessUrl(frontendBaseUrl + "/payment/success?payment_id=" + payment.getId() + "&session_id={CHECKOUT_SESSION_ID}")
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
            RequestOptions requestOptions = RequestOptions.builder()
                    .setApiKey(secretKey)
                    .setIdempotencyKey(payment.getId().toString())
                    .build();
            Session session = Session.create(params, requestOptions);
            if (session.getUrl() == null || session.getUrl().isBlank()) {
                throw new IllegalStateException("Stripe did not return a Checkout URL.");
            }
            return new CheckoutSessionDetails(session.getId(), session.getUrl());
        } catch (StripeException exception) {
            StripeError stripeError = exception.getStripeError();
            String errorCode = stripeError == null ? null : stripeError.getCode();

            log.warn(
                    "Stripe Checkout API request failed: requestId={}, statusCode={}, errorType={}, errorCode={}, errorParam={}, message={}",
                    exception.getRequestId(),
                    exception.getStatusCode(),
                    stripeError == null ? null : stripeError.getType(),
                    errorCode,
                    stripeError == null ? null : stripeError.getParam(),
                    stripeError == null ? exception.getMessage() : stripeError.getMessage(),
                    exception);

            throw new StripeCheckoutException(
                    "Unable to create Stripe Checkout Session.", exception, errorCode);
        }
    }
}