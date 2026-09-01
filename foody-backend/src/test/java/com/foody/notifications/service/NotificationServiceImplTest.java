package com.foody.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foody.common.exception.ResourceNotFoundException;
import com.foody.notifications.dto.NotificationResponse;
import com.foody.notifications.entity.Notification;
import com.foody.notifications.entity.NotificationType;
import com.foody.notifications.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;

    NotificationServiceImpl notificationService;

    static final Long RECIPIENT_ID = 1L;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository);
    }

    private Notification unread(Long id) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientUserId(RECIPIENT_ID);
        n.setType(NotificationType.ORDER_STATUS_CHANGED);
        n.setTitle("به‌روزرسانی سفارش");
        n.setMessage("وضعیت سفارش تغییر کرد.");
        n.setReferenceType("ORDER");
        n.setReferenceId(100L);
        n.setRead(false);
        return n;
    }

    @Test
    void notify_savesNewNotificationForRecipient() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.notify(
                RECIPIENT_ID, NotificationType.NEW_ORDER, "سفارش جدید", "یک سفارش جدید ثبت شد.",
                "ORDER", 100L);

        assertThat(response.type()).isEqualTo(NotificationType.NEW_ORDER);
        assertThat(response.title()).isEqualTo("سفارش جدید");
        assertThat(response.referenceType()).isEqualTo("ORDER");
        assertThat(response.referenceId()).isEqualTo(100L);
        assertThat(response.read()).isFalse();
    }

    @Test
    void getMyNotifications_returnsRecipientsNotifications() {
        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(RECIPIENT_ID))
                .thenReturn(List.of(unread(1L)));

        List<NotificationResponse> result = notificationService.getMyNotifications(RECIPIENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void getUnreadCount_delegatesToRepository() {
        when(notificationRepository.countByRecipientUserIdAndReadFalse(RECIPIENT_ID)).thenReturn(3L);

        long count = notificationService.getUnreadCount(RECIPIENT_ID);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void markAsRead_marksOwnedNotificationAsRead() {
        Notification notification = unread(1L);
        when(notificationRepository.findByIdAndRecipientUserId(1L, RECIPIENT_ID))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(1L, RECIPIENT_ID);

        assertThat(response.read()).isTrue();
    }

    @Test
    void markAsRead_throwsWhenNotOwned() {
        when(notificationRepository.findByIdAndRecipientUserId(1L, RECIPIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, RECIPIENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllAsRead_marksEveryUnreadNotification() {
        Notification first = unread(1L);
        Notification second = unread(2L);
        when(notificationRepository.findByRecipientUserIdAndReadFalse(RECIPIENT_ID))
                .thenReturn(List.of(first, second));

        notificationService.markAllAsRead(RECIPIENT_ID);

        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
        verify(notificationRepository, times(1)).saveAll(List.of(first, second));
    }

    @Test
    void markAllAsRead_savesNothingWhenNoneUnread() {
        when(notificationRepository.findByRecipientUserIdAndReadFalse(RECIPIENT_ID)).thenReturn(List.of());

        notificationService.markAllAsRead(RECIPIENT_ID);

        verify(notificationRepository, never()).save(any());
        verify(notificationRepository, times(1)).saveAll(List.of());
    }
}
