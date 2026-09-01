package com.foody.notifications.service;

import com.foody.notifications.dto.NotificationResponse;
import com.foody.notifications.entity.NotificationType;
import java.util.List;

/**
 * Public contract for the notifications module. Other modules (orders, reservations,
 * businesses) depend on this interface only — never on the entity or repository — and
 * call {@link #notify} at the point a customer- or owner-facing event occurs (status
 * change, new order/reservation, business approval, etc).
 */
public interface NotificationService {

    /**
     * Creates an in-app notification for one recipient. Phase 1: in-app storage only
     * (see decision log) — this call never fails the caller's transaction for delivery
     * reasons, since there is no external delivery channel yet.
     *
     * @param referenceType a short caller-defined tag (e.g. "ORDER", "RESERVATION",
     *                       "BUSINESS") for deep-linking; pass {@code null} if not applicable
     * @param referenceId   the id of the referenced entity; pass {@code null} if not applicable
     */
    NotificationResponse notify(Long recipientUserId, NotificationType type, String title, String message,
                                 String referenceType, Long referenceId);

    List<NotificationResponse> getMyNotifications(Long recipientUserId);

    long getUnreadCount(Long recipientUserId);

    NotificationResponse markAsRead(Long notificationId, Long recipientUserId);

    void markAllAsRead(Long recipientUserId);
}
