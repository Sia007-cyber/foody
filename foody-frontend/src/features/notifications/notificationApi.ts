import { apiRequest } from "../../lib/api";
import type { Notification, UnreadCountResponse } from "../../types/api";

export const notificationApi = {
  my: () => apiRequest<Notification[]>("/api/notifications/my"),

  unreadCount: () => apiRequest<UnreadCountResponse>("/api/notifications/unread-count"),

  markAsRead: (id: number) =>
    apiRequest<Notification>(`/api/notifications/${id}/read`, { method: "PATCH" }),

  markAllAsRead: () => apiRequest<void>("/api/notifications/read-all", { method: "PATCH" }),
};
