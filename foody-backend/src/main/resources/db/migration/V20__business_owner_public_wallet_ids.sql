-- Reuse the existing unique public wallet identity for business owners.
-- Existing customer identifiers and all wallet/ledger data remain unchanged.
UPDATE users
SET public_id = CONCAT('F-', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 16)))
WHERE role = 'BUSINESS_OWNER' AND public_id IS NULL;
