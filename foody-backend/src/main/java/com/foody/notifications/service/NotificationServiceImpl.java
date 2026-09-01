package com.foody.notifications.service;

import com.foody.common.exception.ResourceNotFoundException;
import com.foody.notifications.dto.NotificationResponse;
import com.foody.notifications.entity.Notification;
import com.foody.notifications.entity.NotificationType;
import com.foody.notifications.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public NotificationResponse notify(Long recipientUserId, NotificationType type, String title, String message,
                                        String referenceType, Long referenceId) {
        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long recipientUserId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientUserId) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(recipientUserId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long recipientUserId) {
        // Not-found-or-not-owned collapse to the same 404, same pattern as orders/reservations.
        Notification notification = notificationRepository
                .findByIdAndRecipientUserId(notificationId, recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(Long recipientUserId) {
        List<Notification> unread = notificationRepository.findByRecipientUserIdAndReadFalse(recipientUserId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
