package com.ryanbondoc.fintech.payment.api;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ryanbondoc.fintech.payment.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

@RestController
@RequestMapping("/api/payments/webhooks")
public class StripeWebhookController {
    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    private final PaymentService paymentService;
    private final String webhookSecret;

    public StripeWebhookController(PaymentService paymentService,
            @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.paymentService = paymentService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signature) {
        if (webhookSecret == null || webhookSecret.isBlank() || signature == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException | RuntimeException exception) {
            log.warn("Rejected Stripe webhook: invalid signature or payload");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if ("checkout.session.completed".equals(event.getType())
                || "checkout.session.async_payment_succeeded".equals(event.getType())) {
            StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
            if (!(object instanceof Session session) || !"paid".equalsIgnoreCase(session.getPaymentStatus())) {
                return ResponseEntity.ok().build();
            }
            Map<String, String> metadata = session.getMetadata();
            try {
                if (metadata == null || metadata.get("paymentId") == null || session.getId() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
                paymentService.markStripePaymentSucceeded(UUID.fromString(metadata.get("paymentId")), session.getId());
            } catch (RuntimeException exception) {
                log.error("Unable to reconcile verified Stripe payment event {}", event.getId(), exception);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        return ResponseEntity.ok().build();
    }
}
