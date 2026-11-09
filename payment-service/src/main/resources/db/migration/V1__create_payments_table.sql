CREATE TABLE payments (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0.50),
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    stripe_session_id VARCHAR(255) UNIQUE,
    checkout_url VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payments_customer_created_at
    ON payments (customer_id, created_at DESC);