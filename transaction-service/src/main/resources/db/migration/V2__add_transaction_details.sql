ALTER TABLE financial_transactions
    ADD COLUMN merchant VARCHAR(150),
    ADD COLUMN category VARCHAR(30);