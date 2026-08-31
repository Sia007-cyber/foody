-- V3: Orders module (Phase 1 core flow — pickup & delivery, no online payment yet).

CREATE TABLE orders (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    customer_user_id  BIGINT        NOT NULL,
    business_id       BIGINT        NOT NULL,
    fulfillment_type  ENUM('PICKUP', 'DELIVERY') NOT NULL,
    status            ENUM('PENDING', 'ACCEPTED', 'PREPARING', 'READY', 'COMPLETED', 'REJECTED', 'CANCELLED')
                      NOT NULL DEFAULT 'PENDING',
    delivery_address  VARCHAR(512),
    total_amount      DECIMAL(10, 2) NOT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_orders_customer (customer_user_id),
    KEY idx_orders_business (business_id),
    KEY idx_orders_status (status),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_business FOREIGN KEY (business_id) REFERENCES businesses (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- product_name and unit_price are snapshots taken at order time, so an order's
-- history stays accurate even if the product is later renamed, repriced, or removed.
CREATE TABLE order_items (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    order_id      BIGINT         NOT NULL,
    product_id    BIGINT         NOT NULL,
    product_name  VARCHAR(255)   NOT NULL,
    unit_price    DECIMAL(10, 2) NOT NULL,
    quantity      INT            NOT NULL,
    subtotal      DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order (order_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
