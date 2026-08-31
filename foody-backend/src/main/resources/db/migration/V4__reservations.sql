-- V4: Reservations module (Phase 1 — independent from food ordering, no capacity
-- limit yet; availability just lists existing reservations for a date. See decision log.)

CREATE TABLE reservations (
    id                 BIGINT   NOT NULL AUTO_INCREMENT,
    business_id        BIGINT   NOT NULL,
    customer_user_id   BIGINT   NOT NULL,
    reservation_date   DATE     NOT NULL,
    reservation_time   TIME     NOT NULL,
    guest_count        INT      NOT NULL,
    status             ENUM('PENDING', 'CONFIRMED', 'COMPLETED', 'REJECTED', 'CANCELLED')
                       NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_reservations_business_date (business_id, reservation_date),
    KEY idx_reservations_customer (customer_user_id),
    CONSTRAINT fk_reservations_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_reservations_customer FOREIGN KEY (customer_user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
