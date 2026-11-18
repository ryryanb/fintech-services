ALTER TABLE financial_transactions
    ADD COLUMN category_manually_set BOOLEAN NOT NULL DEFAULT FALSE;
