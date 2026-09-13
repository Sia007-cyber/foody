CREATE TABLE impersonation_sessions (
    id                  VARCHAR(36)  NOT NULL,
    admin_user_id       BIGINT       NOT NULL,
    target_user_id      BIGINT       NOT NULL,
    started_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ended_at            TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY idx_impersonation_admin (admin_user_id),
    KEY idx_impersonation_target (target_user_id),
    CONSTRAINT fk_impersonation_admin FOREIGN KEY (admin_user_id) REFERENCES users (id),
    CONSTRAINT fk_impersonation_target FOREIGN KEY (target_user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

ALTER TABLE refresh_token_sessions
    ADD COLUMN impersonation_session_id VARCHAR(36) NULL,
    ADD KEY idx_refresh_impersonation (impersonation_session_id),
    ADD CONSTRAINT fk_refresh_impersonation FOREIGN KEY (impersonation_session_id)
        REFERENCES impersonation_sessions (id);
