-- V1: Core Phase 0 schema for Foody platform.
-- Spec tables: users, businesses, business_hours, menus, products
-- Plus business_types reference table (enables business_type extensibility WITHOUT ALTER TABLE).

CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(64),
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            ENUM('CUSTOMER', 'BUSINESS_OWNER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    status          ENUM('ACTIVE', 'SUSPENDED')                 NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Reference table for business types. Adding a new type later = INSERT a row here
-- + add a Java enum constant. No ALTER TABLE on businesses is ever required.
CREATE TABLE business_types (
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(255) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE businesses (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    owner_user_id   BIGINT       NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    business_type   VARCHAR(50)  NOT NULL,
    address         VARCHAR(512),
    latitude        DECIMAL(10, 8),
    longitude       DECIMAL(11, 8),
    phone           VARCHAR(64),
    status          ENUM('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED') NOT NULL DEFAULT 'PENDING',
    cover_image_url VARCHAR(512),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_businesses_type (business_type),
    KEY idx_businesses_status (status),
    CONSTRAINT fk_businesses_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_businesses_type  FOREIGN KEY (business_type) REFERENCES business_types (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE business_hours (
    id           BIGINT    NOT NULL AUTO_INCREMENT,
    business_id  BIGINT    NOT NULL,
    day_of_week  TINYINT   NOT NULL, -- 1=MONDAY .. 7=SUNDAY (java.time.DayOfWeek.getValue())
    open_time    TIME,
    close_time   TIME,
    is_closed    BOOLEAN   NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_bh_business (business_id),
    CONSTRAINT fk_bh_business FOREIGN KEY (business_id) REFERENCES businesses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE menus (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    business_id   BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_menus_business (business_id),
    CONSTRAINT fk_menus_business FOREIGN KEY (business_id) REFERENCES businesses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE products (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    menu_id        BIGINT        NOT NULL,
    name           VARCHAR(255)  NOT NULL,
    description    TEXT,
    price          DECIMAL(10, 2) NOT NULL,
    image_url      VARCHAR(512),
    is_available   BOOLEAN       NOT NULL DEFAULT TRUE,
    display_order  INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_products_menu (menu_id),
    CONSTRAINT fk_products_menu FOREIGN KEY (menu_id) REFERENCES menus (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
