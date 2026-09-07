CREATE TABLE offers (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    business_id BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    capacity    INT          NOT NULL,
    starts_at   DATETIME(6)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    status      ENUM('ACTIVE', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version     BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_offers_business (business_id),
    KEY idx_offers_availability (status, starts_at, expires_at),
    CONSTRAINT chk_offers_capacity CHECK (capacity > 0),
    CONSTRAINT chk_offers_time_range CHECK (expires_at > starts_at),
    CONSTRAINT fk_offers_business FOREIGN KEY (business_id) REFERENCES businesses (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE offer_claims (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    offer_id         BIGINT      NOT NULL,
    customer_user_id BIGINT      NOT NULL,
    claimed_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_offer_claims_offer_customer (offer_id, customer_user_id),
    KEY idx_offer_claims_customer (customer_user_id, claimed_at),
    KEY idx_offer_claims_offer (offer_id),
    CONSTRAINT fk_offer_claims_offer FOREIGN KEY (offer_id) REFERENCES offers (id),
    CONSTRAINT fk_offer_claims_customer FOREIGN KEY (customer_user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
