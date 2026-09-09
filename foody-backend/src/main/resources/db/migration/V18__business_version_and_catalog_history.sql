-- Prevent concurrent moderation decisions from overwriting each other.
ALTER TABLE businesses
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Order item names/prices are immutable snapshots. Detach the optional live catalog
-- reference when a product is removed, preserving the complete historical order.
ALTER TABLE order_items
    DROP FOREIGN KEY fk_order_items_product;

ALTER TABLE order_items
    MODIFY product_id BIGINT NULL;

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL;
