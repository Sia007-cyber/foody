import { useEffect, useRef, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notificationApi } from "./notificationApi";
import { useAuth } from "../auth/AuthContext";
import { BellIcon, ReceiptIcon, CalendarCheckIcon, StoreIcon } from "../../components/icons";
import { Spinner, EmptyState } from "../../components/Controls";
import { formatRelativeTime } from "../../lib/format";
import type { Notification } from "../../types/api";

const TYPE_ICON: Record<Notification["type"], ReactNode> = {
  ORDER_STATUS_CHANGED: <ReceiptIcon size={16} />,
  NEW_ORDER: <ReceiptIcon size={16} />,
  RESERVATION_STATUS_CHANGED: <CalendarCheckIcon size={16} />,
  NEW_RESERVATION: <CalendarCheckIcon size={16} />,
  BUSINESS_STATUS_CHANGED: <StoreIcon size={16} />,
};

/** Resolves where a notification should deep-link to, based on the viewer's role. */
function resolveLink(n: Notification, role: string | undefined): string | null {
  if (!n.referenceType) return null;
  if (role === "CUSTOMER") {
    if (n.referenceType === "ORDER") return `/orders/${n.referenceId}`;
    if (n.referenceType === "RESERVATION") return "/reservations";
  }
  if (role === "BUSINESS_OWNER") {
    if (n.referenceType === "ORDER") return "/business/orders";
    if (n.referenceType === "RESERVATION") return "/business/reservations";
    if (n.referenceType === "BUSINESS") return "/business/profile";
  }
  if (role === "ADMIN" && n.referenceType === "BUSINESS") return "/admin/businesses";
  return null;
}

export function NotificationBell() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  const { data: unread } = useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: notificationApi.unreadCount,
    enabled: Boolean(user),
    refetchInterval: 30_000,
  });

  const { data: notifications, isLoading } = useQuery({
    queryKey: ["notifications", "my"],
    queryFn: notificationApi.my,
    enabled: Boolean(user) && open,
  });

  const markAsRead = useMutation({
    mutationFn: (id: number) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });

  const markAllAsRead = useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    if (open) document.addEventListener("mousedown", onClickOutside);
    return () => document.removeEventListener("mousedown", onClickOutside);
  }, [open]);

  if (!user) return null;

  const unreadCount = unread?.unreadCount ?? 0;

  function handleItemClick(n: Notification) {
    if (!n.read) markAsRead.mutate(n.id);
    const link = resolveLink(n, user?.role);
    if (link) {
      navigate(link);
      setOpen(false);
    }
  }

  return (
    <div className="notif-bell-root" ref={rootRef}>
      <button
        type="button"
        className="notif-bell"
        aria-label="اعلان‌ها"
        onClick={() => setOpen((v) => !v)}
      >
        <BellIcon size={19} />
        {unreadCount > 0 && (
          <span className="notif-bell-badge">{unreadCount > 9 ? "۹+" : new Intl.NumberFormat("fa-IR").format(unreadCount)}</span>
        )}
      </button>

      {open && (
        <div className="notif-dropdown" role="menu">
          <div className="notif-dropdown-header">
            <span>اعلان‌ها</span>
            {unreadCount > 0 && (
              <button
                type="button"
                className="notif-mark-all"
                onClick={() => markAllAsRead.mutate()}
                disabled={markAllAsRead.isPending}
              >
                علامت‌گذاری همه به‌عنوان خوانده‌شده
              </button>
            )}
          </div>

          <div className="notif-dropdown-list">
            {isLoading ? (
              <div className="notif-dropdown-loading">
                <Spinner />
              </div>
            ) : !notifications || notifications.length === 0 ? (
              <EmptyState title="فعلاً اعلانی نداری" />
            ) : (
              notifications.map((n) => (
                <button
                  key={n.id}
                  type="button"
                  className={`notif-item ${n.read ? "" : "unread"}`}
                  onClick={() => handleItemClick(n)}
                >
                  <span className="notif-item-icon">{TYPE_ICON[n.type]}</span>
                  <span className="notif-item-body">
                    <span className="notif-item-title">{n.title}</span>
                    <span className="notif-item-message">{n.message}</span>
                    <span className="notif-item-time">{formatRelativeTime(n.createdAt)}</span>
                  </span>
                  {!n.read && <span className="notif-item-dot" />}
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
