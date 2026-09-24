-- Extends the existing support-ticket model so CUSTOMER users can open tickets too,
-- without duplicating the schema. All existing BUSINESS_OWNER tickets are preserved:
-- the rename keeps every row's data, and the new requester_type column backfills to
-- 'BUSINESS_OWNER' for every ticket that already exists.

ALTER TABLE support_tickets
    CHANGE COLUMN owner_user_id requester_user_id BIGINT NOT NULL,
    CHANGE COLUMN owner_last_read_at requester_last_read_at TIMESTAMP NULL,
    MODIFY COLUMN business_id BIGINT NULL,
    ADD COLUMN requester_type ENUM('BUSINESS_OWNER', 'CUSTOMER') NOT NULL DEFAULT 'BUSINESS_OWNER' AFTER requester_user_id;

ALTER TABLE support_tickets
    ADD KEY idx_support_tickets_requester (requester_type, requester_user_id, updated_at DESC);

ALTER TABLE support_ticket_messages
    MODIFY COLUMN sender_type ENUM('BUSINESS_OWNER', 'ADMIN', 'CUSTOMER') NOT NULL;
