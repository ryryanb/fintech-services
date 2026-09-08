CREATE TABLE financial_transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_financial_transactions_account_id
    ON financial_transactions(account_id);

CREATE INDEX idx_financial_transactions_account_date
    ON financial_transactions(account_id, transaction_date DESC);