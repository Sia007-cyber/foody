-- Public, shareable customer identifier. Non-customers intentionally keep NULL.
ALTER TABLE users ADD COLUMN public_id VARCHAR(19) NULL AFTER id;

-- UUID entropy makes the backfill independent of sequential primary keys. The
-- unique key is installed before the column is used by the application.
UPDATE users
SET public_id = CONCAT('F-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 16)))
WHERE role = 'CUSTOMER' AND public_id IS NULL;

ALTER TABLE users ADD CONSTRAINT uk_users_public_id UNIQUE (public_id);
