ALTER TABLE customers
ADD COLUMN user_id UUID;

CREATE UNIQUE INDEX ux_customers_user_id
ON customers(user_id);