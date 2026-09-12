-- Retire online ordering at the application layer while preserving historical tables.
-- Existing accounts may keep a null phone; every new registration is enforced by the API.
ALTER TABLE users MODIFY email VARCHAR(255) NULL;
ALTER TABLE users ADD UNIQUE KEY uk_users_phone (phone);

ALTER TABLE businesses ADD COLUMN manager_national_id VARCHAR(255) NULL AFTER owner_user_id;

CREATE TABLE wallet_purchase_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    debit_request_id BIGINT NOT NULL,
    product_id BIGINT NULL,
    product_name_snapshot VARCHAR(255) NOT NULL,
    unit_price_snapshot DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_purchase_items_request (debit_request_id),
    CONSTRAINT fk_purchase_item_request FOREIGN KEY (debit_request_id)
        REFERENCES owner_debit_requests(id),
    CONSTRAINT fk_purchase_item_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE SET NULL,
    CONSTRAINT chk_purchase_item_price CHECK (unit_price_snapshot >= 0),
    CONSTRAINT chk_purchase_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_purchase_item_total CHECK (line_total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_debit_business_status_created
    ON owner_debit_requests(business_id, status, created_at);
