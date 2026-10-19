CREATE TABLE spending_categories (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    name VARCHAR(60) NOT NULL,
    CONSTRAINT uk_spending_categories_customer_name UNIQUE (customer_id, name)
);

CREATE INDEX idx_spending_categories_customer_id
    ON spending_categories(customer_id);
