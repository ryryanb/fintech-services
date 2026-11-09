CREATE TABLE plaid_connections (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    item_id VARCHAR(100) NOT NULL UNIQUE,
    institution_id VARCHAR(100),
    institution_name VARCHAR(150),
    encrypted_access_token TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE financial_accounts
    ADD COLUMN external_connection_id UUID,
    ADD COLUMN provider_account_id VARCHAR(100);

CREATE UNIQUE INDEX uq_financial_accounts_provider_account_id
    ON financial_accounts (provider_account_id)
    WHERE provider_account_id IS NOT NULL;

CREATE INDEX idx_plaid_connections_customer_id
    ON plaid_connections (customer_id);
