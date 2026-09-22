ALTER TABLE reviews
    ADD COLUMN moderation_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'APPROVED',
    ADD KEY idx_reviews_moderation_created (moderation_status, created_at DESC, id DESC);

ALTER TABLE product_reviews
    ADD COLUMN moderation_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'APPROVED',
    ADD KEY idx_product_reviews_moderation_created (moderation_status, created_at DESC, id DESC);

-- Existing rows were already publicly visible before moderation existed, so preserve
-- that production behavior. New entity writes explicitly persist PENDING.
