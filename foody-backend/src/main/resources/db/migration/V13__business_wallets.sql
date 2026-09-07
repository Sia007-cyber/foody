-- Global V7 balances cannot be assigned to a business safely. Preserve them and
-- their ledgers as explicit read-only legacy tables; new scoped wallets start empty.
RENAME TABLE wallet_transactions TO legacy_wallet_transactions, wallets TO legacy_wallets;

CREATE TABLE wallets (
 id BIGINT NOT NULL AUTO_INCREMENT, customer_user_id BIGINT NOT NULL, business_id BIGINT NOT NULL,
 balance DECIMAL(12,2) NOT NULL DEFAULT 0.00, version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uq_wallet_customer_business(customer_user_id,business_id), KEY idx_wallet_business(business_id),
 CONSTRAINT fk_wallet_customer FOREIGN KEY(customer_user_id) REFERENCES users(id),
 CONSTRAINT fk_wallet_business FOREIGN KEY(business_id) REFERENCES businesses(id),
 CONSTRAINT chk_business_wallet_balance CHECK(balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE owner_debit_requests (
 id BIGINT NOT NULL AUTO_INCREMENT, wallet_id BIGINT NOT NULL, customer_user_id BIGINT NOT NULL,
 business_id BIGINT NOT NULL, requested_by_owner_user_id BIGINT NOT NULL, amount DECIMAL(12,2) NOT NULL,
 status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, resolved_at TIMESTAMP NULL,
 PRIMARY KEY(id), KEY idx_debit_customer_status(customer_user_id,status,created_at),
 CONSTRAINT fk_debit_wallet FOREIGN KEY(wallet_id) REFERENCES wallets(id),
 CONSTRAINT fk_debit_customer FOREIGN KEY(customer_user_id) REFERENCES users(id),
 CONSTRAINT fk_debit_business FOREIGN KEY(business_id) REFERENCES businesses(id),
 CONSTRAINT fk_debit_owner FOREIGN KEY(requested_by_owner_user_id) REFERENCES users(id),
 CONSTRAINT chk_debit_amount CHECK(amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wallet_transactions (
 id BIGINT NOT NULL AUTO_INCREMENT, wallet_id BIGINT NOT NULL, amount DECIMAL(12,2) NOT NULL,
 type ENUM('OWNER_CREDIT','OWNER_DEBIT','ADMIN_CREDIT','ADMIN_DEBIT') NOT NULL,
 actor_user_id BIGINT NOT NULL, actor_type ENUM('OWNER','ADMIN') NOT NULL,
 balance_after DECIMAL(12,2) NOT NULL, debit_request_id BIGINT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id), KEY idx_wallet_tx_wallet_created(wallet_id,created_at),
 CONSTRAINT fk_wallet_tx_wallet FOREIGN KEY(wallet_id) REFERENCES wallets(id),
 CONSTRAINT fk_wallet_tx_actor FOREIGN KEY(actor_user_id) REFERENCES users(id),
 CONSTRAINT fk_wallet_tx_request FOREIGN KEY(debit_request_id) REFERENCES owner_debit_requests(id),
 CONSTRAINT chk_wallet_tx_amount CHECK(amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
