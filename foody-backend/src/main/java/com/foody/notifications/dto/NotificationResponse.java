package com.foody.notifications.dto;

import com.foody.notifications.entity.Notification;
import com.foody.notifications.entity.NotificationType;
import java.time.Instant;

/** Public view of a notification. Returned instead of the entity. */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String referenceType,
        Long referenceId,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getReferenceType(), n.getReferenceId(), n.isRead(), n.getCreatedAt());
    }
}
