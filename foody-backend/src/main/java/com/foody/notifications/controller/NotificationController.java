package com.foody.notifications.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.notifications.dto.NotificationResponse;
import com.foody.notifications.dto.UnreadCountResponse;
import com.foody.notifications.service.NotificationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notification endpoints, shared by all three panels (customer, business owner, admin) —
 * everyone reads only their own notifications, keyed off the authenticated principal.
 * Every route here requires authentication (no public matcher needed in WebSecurityConfig).
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/my")
    public List<NotificationResponse> myNotifications(@AuthenticationPrincipal FoodyUserPrincipal principal) {
        return notificationService.getMyNotifications(principal.getUserId());
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal FoodyUserPrincipal principal) {
        return new UnreadCountResponse(notificationService.getUnreadCount(principal.getUserId()));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                            @PathVariable Long id) {
        return notificationService.markAsRead(id, principal.getUserId());
    }

    @PatchMapping("/read-all")
    public void markAllAsRead(@AuthenticationPrincipal FoodyUserPrincipal principal) {
        notificationService.markAllAsRead(principal.getUserId());
    }
}
