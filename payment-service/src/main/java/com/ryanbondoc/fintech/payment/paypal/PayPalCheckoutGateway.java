package com.ryanbondoc.fintech.payment.paypal;

import java.math.RoundingMode;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ryanbondoc.fintech.payment.domain.Payment;
import com.ryanbondoc.fintech.payment.stripe.CheckoutGateway;
import com.ryanbondoc.fintech.payment.stripe.CheckoutSessionDetails;

@Component("paypalCheckoutGateway")
public class PayPalCheckoutGateway implements CheckoutGateway {
    private static final Logger log = LoggerFactory.getLogger(PayPalCheckoutGateway.class);
    private final RestClient paypalClient;
    private final String clientId;
    private final String clientSecret;
    private final String frontendBaseUrl;

    public PayPalCheckoutGateway(
            RestClient.Builder restClientBuilder,
            @Value("${paypal.base-url:https://api-m.sandbox.paypal.com}") String baseUrl,
            @Value("${paypal.client-id:}") String clientId,
            @Value("${paypal.client-secret:}") String clientSecret,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.paypalClient = restClientBuilder.clone().baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public CheckoutSessionDetails createSession(Payment payment) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("PayPal sandbox is not configured. Set PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET.");
        }
        try {
            AccessTokenResponse token = paypalClient.post()
                    .uri("/v1/oauth2/token")
                    .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(AccessTokenResponse.class);
            if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
                throw new IllegalStateException("PayPal did not return an OAuth access token.");
            }

            String amount = payment.getAmount().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
            Map<String, Object> context = Map.of(
                    "return_url", frontendBaseUrl + "/payment/paypal/return?paymentId=" + payment.getId(),
                    "cancel_url", frontendBaseUrl + "/payment/paypal/cancel?paymentId=" + payment.getId(),
                    "user_action", "PAY_NOW",
                    "shipping_preference", "NO_SHIPPING");
            Map<String, Object> request = Map.of(
                    "intent", "CAPTURE",
                    "purchase_units", List.of(Map.of(
                            "reference_id", payment.getId().toString(),
                            "description", payment.getDescription(),
                            "amount", Map.of("currency_code", payment.getCurrency(), "value", amount))),
                    "payment_source", Map.of("paypal", Map.of("experience_context", context)));

            PayPalOrderResponse order = paypalClient.post()
                    .uri("/v2/checkout/orders")
                    .headers(headers -> {
                        headers.setBearerAuth(token.accessToken());
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.set("PayPal-Request-Id", payment.getId().toString());
                    })
                    .body(request)
                    .retrieve()
                    .body(PayPalOrderResponse.class);

            if (order == null || order.id() == null || order.id().isBlank()) {
                throw new IllegalStateException("PayPal did not return an order ID.");
            }
            String approvalUrl = order.links() == null ? null : order.links().stream()
                    .filter(link -> "approve".equalsIgnoreCase(link.rel()) || "payer-action".equalsIgnoreCase(link.rel()))
                    .map(PayPalLink::href)
                    .filter(href -> href != null && !href.isBlank())
                    .findFirst().orElse(null);
            if (!isSandboxApprovalUrl(approvalUrl)) {
                throw new IllegalStateException("PayPal did not return a valid sandbox approval URL.");
            }
            return new CheckoutSessionDetails(order.id(), approvalUrl);
        } catch (RestClientResponseException exception) {
            log.warn("PayPal Checkout API request failed: statusCode={}, response={}",
                    exception.getStatusCode().value(), exception.getResponseBodyAsString());
            throw new PayPalCheckoutException("Unable to create PayPal Checkout order.", exception);
        }
    }

    private boolean isSandboxApprovalUrl(String url) {
        if (url == null) return false;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && ("www.sandbox.paypal.com".equalsIgnoreCase(host)
                    || "sandbox.paypal.com".equalsIgnoreCase(host));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AccessTokenResponse(@JsonProperty("access_token") String accessToken) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PayPalOrderResponse(String id, List<PayPalLink> links) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PayPalLink(String href, String rel) { }
}
