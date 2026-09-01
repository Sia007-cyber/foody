-- V6: Notifications module (Phase 1 — in-app only, no push/SMS/email delivery yet;
-- see decision log). One row per recipient per event; reference_type/reference_id
-- let the frontend deep-link to the order/reservation/business that triggered it.

CREATE TABLE notifications (
    id                 BIGINT   NOT NULL AUTO_INCREMENT,
    recipient_user_id  BIGINT   NOT NULL,
    type               ENUM('ORDER_STATUS_CHANGED', 'NEW_ORDER',
                             'RESERVATION_STATUS_CHANGED', 'NEW_RESERVATION',
                             'BUSINESS_STATUS_CHANGED')
                       NOT NULL,
    title              VARCHAR(200)  NOT NULL,
    message            VARCHAR(1000) NOT NULL,
    reference_type     VARCHAR(50),
    reference_id       BIGINT,
    is_read            BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notifications_recipient_created (recipient_user_id, created_at DESC),
    KEY idx_notifications_recipient_unread (recipient_user_id, is_read),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
