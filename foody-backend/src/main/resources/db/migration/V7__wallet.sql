-- V7: Wallet module (Phase 2 kickoff). One wallet per user, created lazily on first
-- access (see WalletServiceImpl#getOrCreateWallet) rather than at registration time.
-- Top-up is currently simulated — no real payment gateway wired in yet (locked scope
-- decision, see root README); it directly credits the wallet on request.

CREATE TABLE wallets (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    balance     DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    version     BIGINT   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_wallets_user (user_id),
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_wallets_balance_non_negative CHECK (balance >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE wallet_transactions (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    wallet_id       BIGINT   NOT NULL,
    type            ENUM('TOPUP', 'ORDER_PAYMENT', 'REFUND', 'CREDIT_REWARD', 'ADMIN_ADJUSTMENT')
                    NOT NULL,
    is_credit       BOOLEAN  NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    balance_after   DECIMAL(12, 2) NOT NULL,
    description     VARCHAR(300),
    reference_type  VARCHAR(50),
    reference_id    BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_wallet_transactions_wallet_created (wallet_id, created_at DESC),
    CONSTRAINT fk_wallet_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT chk_wallet_transactions_amount_positive CHECK (amount > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
