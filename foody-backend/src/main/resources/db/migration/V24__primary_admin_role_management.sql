-- Primary-admin capability and immutable role-change audit.
-- No production identity is seeded here; provisioning is performed by the
-- explicitly enabled, non-web application runner.
ALTER TABLE users
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN is_primary_admin BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN admin_base_role ENUM('CUSTOMER', 'BUSINESS_OWNER') NULL,
    ADD COLUMN primary_admin_singleton TINYINT
        GENERATED ALWAYS AS (CASE WHEN is_primary_admin THEN 1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_users_single_primary_admin (primary_admin_singleton),
    ADD CONSTRAINT chk_users_primary_admin_role
        CHECK (is_primary_admin = FALSE OR (role = 'ADMIN' AND status = 'ACTIVE')),
    ADD CONSTRAINT chk_users_admin_base_role
        CHECK (admin_base_role IS NULL OR role = 'ADMIN');

-- Historical ordinary admins did not retain a prior role. CUSTOMER is the
-- conservative least-privileged role to restore if one is later revoked.
UPDATE users
SET admin_base_role = 'CUSTOMER'
WHERE role = 'ADMIN' AND is_primary_admin = FALSE;

CREATE TABLE admin_role_audit (
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    actor_primary_admin_user_id BIGINT       NOT NULL,
    target_user_id              BIGINT       NOT NULL,
    previous_role               VARCHAR(32)  NULL,
    new_role                    VARCHAR(32)  NOT NULL,
    action_type                 VARCHAR(40)  NOT NULL,
    created_at                  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_admin_role_audit_actor (actor_primary_admin_user_id),
    KEY idx_admin_role_audit_target (target_user_id),
    KEY idx_admin_role_audit_created (created_at),
    CONSTRAINT fk_admin_role_audit_actor FOREIGN KEY (actor_primary_admin_user_id)
        REFERENCES users (id),
    CONSTRAINT fk_admin_role_audit_target FOREIGN KEY (target_user_id)
        REFERENCES users (id),
    CONSTRAINT chk_admin_role_audit_previous_role
        CHECK (previous_role IS NULL OR previous_role IN ('CUSTOMER', 'BUSINESS_OWNER', 'ADMIN')),
    CONSTRAINT chk_admin_role_audit_new_role
        CHECK (new_role IN ('CUSTOMER', 'BUSINESS_OWNER', 'ADMIN')),
    CONSTRAINT chk_admin_role_audit_action
        CHECK (action_type IN ('GRANT_ADMIN', 'REVOKE_ADMIN', 'PROVISION_PRIMARY_ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
