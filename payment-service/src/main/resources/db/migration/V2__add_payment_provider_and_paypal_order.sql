ALTER TABLE payments
    ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'STRIPE',
    ADD COLUMN paypal_order_id VARCHAR(255);

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_provider CHECK (provider IN ('STRIPE', 'PAYPAL'));

CREATE UNIQUE INDEX uq_payments_paypal_order_id
    ON payments (paypal_order_id)
    WHERE paypal_order_id IS NOT NULL;
