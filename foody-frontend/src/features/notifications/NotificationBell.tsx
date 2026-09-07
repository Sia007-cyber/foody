import { useEffect, useRef, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notificationApi } from "./notificationApi";
import { useAuth } from "../auth/AuthContext";
import { BellIcon, CloseIcon, ReceiptIcon, CalendarCheckIcon, StoreIcon } from "../../components/icons";
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

const MOBILE_QUERY = "(max-width: 720px)";

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
  const dropdownRef = useRef<HTMLDivElement>(null);
  const [desktopPos, setDesktopPos] = useState<{ top: number; left: number } | null>(null);

  const { data: unread } = useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: notificationApi.unreadCount,
    enabled: Boolean(user),
    refetchInterval: 30_000,
  });

  const {
    data: notifications,
    isLoading,
    isError,
    refetch,
  } = useQuery({
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

  // موقع باز شدن، اگه سایز صفحه دسکتاپه، جای دراپ‌داون رو زیر آیکون زنگ محاسبه کن
  // (چون دراپ‌داون الان با پورتال بیرون از هدر رندر می‌شه تا زیر هیرو گیر نکنه)
  useEffect(() => {
    if (!open) return;
    function updatePosition() {
      if (window.matchMedia(MOBILE_QUERY).matches) {
        setDesktopPos(null);
        return;
      }
      const rect = rootRef.current?.getBoundingClientRect();
      if (rect) {
        setDesktopPos({ top: rect.bottom + 10, left: rect.left });
      }
    }
    updatePosition();
    window.addEventListener("resize", updatePosition);
    return () => window.removeEventListener("resize", updatePosition);
  }, [open]);

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      const target = e.target as Node;
      const insideRoot = rootRef.current?.contains(target);
      const insideDropdown = dropdownRef.current?.contains(target);
      if (!insideRoot && !insideDropdown) {
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

      {open &&
        createPortal(
          <>
            <div className="notif-backdrop" onClick={() => setOpen(false)} />
            <div
              className="notif-dropdown"
              role="menu"
              ref={dropdownRef}
              style={desktopPos ? { top: desktopPos.top, left: desktopPos.left } : undefined}
            >
              <div className="notif-dropdown-header">
                <span>اعلان‌ها</span>
                <div className="notif-dropdown-header-actions">
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
                  <button
                    type="button"
                    className="notif-close-btn"
                    aria-label="بستن اعلان‌ها"
                    onClick={() => setOpen(false)}
                  >
                    <CloseIcon size={16} />
                  </button>
                </div>
              </div>

              <div className="notif-dropdown-list">
                {isLoading ? (
                  <div className="notif-dropdown-loading">
                    <Spinner />
                  </div>
                ) : isError ? (
                  <div className="notif-dropdown-loading">
                    <button type="button" className="notif-mark-all" onClick={() => refetch()}>
                      اعلان‌ها لود نشد — تلاش دوباره
                    </button>
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
          </>,
          document.body,
        )}
    </div>
  );
}
