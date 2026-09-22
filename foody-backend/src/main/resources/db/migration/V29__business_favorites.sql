CREATE TABLE business_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_user_id BIGINT NOT NULL,
    business_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_favorites_customer_business (customer_user_id, business_id),
    KEY idx_business_favorites_customer_created (customer_user_id, created_at),
    KEY idx_business_favorites_business (business_id),
    CONSTRAINT fk_business_favorites_customer FOREIGN KEY (customer_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_business_favorites_business FOREIGN KEY (business_id) REFERENCES businesses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
