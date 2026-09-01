package com.foody.notifications.dto;

/** Lightweight payload for a notification bell badge — avoids shipping full rows just to count. */
public record UnreadCountResponse(long unreadCount) {
}
