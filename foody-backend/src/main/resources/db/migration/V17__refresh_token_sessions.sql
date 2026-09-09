CREATE TABLE refresh_token_sessions (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL,
    token_hash          VARCHAR(64)  NOT NULL,
    expires_at          TIMESTAMP(6) NOT NULL,
    revoked_at          TIMESTAMP(6) NULL,
    replaced_by_hash    VARCHAR(64)  NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_sessions_hash (token_hash),
    KEY idx_refresh_token_sessions_user (user_id),
    KEY idx_refresh_token_sessions_expiry (expires_at),
    CONSTRAINT fk_refresh_token_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
