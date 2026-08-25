CREATE TABLE financial_accounts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL,
    institution_name VARCHAR(100),
    status VARCHAR(20) NOT NULL
);