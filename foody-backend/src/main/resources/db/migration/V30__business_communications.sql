CREATE TABLE support_tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    business_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    subject VARCHAR(160) NOT NULL,
    status ENUM('OPEN', 'ANSWERED', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    owner_last_read_at TIMESTAMP NULL,
    admin_last_read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_support_tickets_business_updated (business_id, updated_at DESC),
    KEY idx_support_tickets_status_updated (status, updated_at DESC),
    CONSTRAINT fk_support_tickets_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_support_tickets_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE support_ticket_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    sender_type ENUM('BUSINESS_OWNER', 'ADMIN') NOT NULL,
    body VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_support_ticket_messages_ticket_created (ticket_id, created_at, id),
    CONSTRAINT fk_support_ticket_messages_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (id),
    CONSTRAINT fk_support_ticket_messages_sender FOREIGN KEY (sender_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE business_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_business_id BIGINT NULL,
    audience_business_id_cutoff BIGINT NULL,
    sender_admin_user_id BIGINT NOT NULL,
    message_type ENUM('DIRECT', 'BROADCAST') NOT NULL,
    subject VARCHAR(160) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_business_messages_target_created (target_business_id, created_at DESC),
    KEY idx_business_messages_type_created (message_type, created_at DESC),
    CONSTRAINT fk_business_messages_target FOREIGN KEY (target_business_id) REFERENCES businesses (id),
    CONSTRAINT fk_business_messages_sender FOREIGN KEY (sender_admin_user_id) REFERENCES users (id),
    CONSTRAINT chk_business_messages_target CHECK (
        (message_type = 'DIRECT' AND target_business_id IS NOT NULL AND audience_business_id_cutoff IS NULL) OR
        (message_type = 'BROADCAST' AND target_business_id IS NULL AND audience_business_id_cutoff IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE business_message_reads (
    message_id BIGINT NOT NULL,
    business_id BIGINT NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id, business_id),
    KEY idx_business_message_reads_business (business_id, read_at DESC),
    CONSTRAINT fk_business_message_reads_message FOREIGN KEY (message_id) REFERENCES business_messages (id),
    CONSTRAINT fk_business_message_reads_business FOREIGN KEY (business_id) REFERENCES businesses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE notifications MODIFY type ENUM(
    'ORDER_STATUS_CHANGED', 'NEW_ORDER', 'RESERVATION_STATUS_CHANGED',
    'NEW_RESERVATION', 'BUSINESS_STATUS_CHANGED', 'SUPPORT_TICKET',
    'SUPPORT_REPLY', 'ADMIN_MESSAGE'
) NOT NULL;
