package com.foody.notifications.entity;

/**
 * The kind of event a notification reports. Drives which reference type/id it carries
 * and gives the frontend a stable key to pick an icon/label without parsing the message.
 */
public enum NotificationType {
    ORDER_STATUS_CHANGED,
    NEW_ORDER,
    RESERVATION_STATUS_CHANGED,
    NEW_RESERVATION,
    BUSINESS_STATUS_CHANGED
}
