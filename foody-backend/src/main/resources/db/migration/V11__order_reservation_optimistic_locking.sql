-- Prevent concurrent lifecycle transitions from silently overwriting each other.
-- Existing rows start at version 0; Hibernate increments the value on each update.
ALTER TABLE orders
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE reservations
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
