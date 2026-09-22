ALTER TABLE businesses ADD COLUMN city VARCHAR(100) NULL AFTER address;
CREATE INDEX idx_businesses_public_city ON businesses (status, city);
