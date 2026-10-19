ALTER TABLE financial_transactions
    ALTER COLUMN category TYPE VARCHAR(60)
    USING category::text;
