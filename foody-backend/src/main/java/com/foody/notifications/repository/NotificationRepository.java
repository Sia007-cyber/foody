package com.foody.notifications.repository;

import com.foody.notifications.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    long countByRecipientUserIdAndReadFalse(Long recipientUserId);

    List<Notification> findByRecipientUserIdAndReadFalse(Long recipientUserId);
}
