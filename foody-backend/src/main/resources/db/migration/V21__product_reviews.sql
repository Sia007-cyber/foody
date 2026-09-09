CREATE TABLE product_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    reviewer_user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(2000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_product_reviews_product_reviewer UNIQUE (product_id, reviewer_user_id),
    KEY idx_product_reviews_product_created (product_id, created_at),
    CONSTRAINT fk_product_reviews_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_reviews_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_product_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
