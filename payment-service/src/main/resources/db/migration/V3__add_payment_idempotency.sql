ALTER TABLE payments
    ADD COLUMN idempotency_key VARCHAR(128),
    ADD COLUMN request_fingerprint VARCHAR(64);

CREATE UNIQUE INDEX uq_payments_customer_idempotency_key
    ON payments (customer_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
