import type { ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { businessApi } from "../businesses/businessApi";
import { orderApi } from "../orders/orderApi";
import { reservationApi } from "../reservations/reservationApi";
import { DashboardShell } from "../../components/DashboardShell";
import { useToast } from "../../components/Feedback";
import { Panel, PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { BusinessStatusBadge, OrderStatusBadge, ReservationStatusBadge } from "../../components/Badge";
import { formatDateTime, formatTime, formatToman } from "../../lib/format";
import { resolveMediaUrl } from "../../lib/api";
import { ownerNavItems } from "./ownerNav";
import { getLast30DayMetrics, localDateKey } from "./dashboardMetrics";
import {
  ReceiptIcon,
  CalendarCheckIcon,
  WalletIcon,
  StoreIcon,
  MegaphoneIcon,
  ChatIcon,
  MenuBookIcon,
  ChartIcon,
} from "../../components/icons";
import type { Order, Reservation } from "../../types/api";

const ACTIVE_ORDER_STATUSES = new Set(["ACCEPTED", "PREPARING", "READY"]);

type Accent = "ember" | "violet" | "pistachio";

const QUICK_ACTIONS: { to: string; label: string; icon: ReactNode; accent: Accent }[] = [
  { to: "/business/discounts", label: "ایجاد تخفیف", icon: <MegaphoneIcon size={20} />, accent: "violet" },
  { to: "/business/reviews", label: "مشاهده نظرات", icon: <ChatIcon size={20} />, accent: "ember" },
  { to: "/business/menus", label: "مدیریت منو", icon: <MenuBookIcon size={20} />, accent: "pistachio" },
  { to: "/business/reports", label: "گزارش فروش", icon: <ChartIcon size={20} />, accent: "violet" },
  { to: "/business/manual-order", label: "ثبت سفارش دستی", icon: <ReceiptIcon size={20} />, accent: "pistachio" },
];

export function OwnerDashboardPage() {
  const { notify } = useToast();
  const {
    data: business,
    isLoading: loadingBusiness,
    isError: businessErrored,
    error: businessError,
    refetch: refetchBusiness,
  } = useQuery({
    queryKey: ["business", "profile"],
    queryFn: businessApi.myProfile,
  });
  const {
    data: orders,
    isLoading: loadingOrders,
    isError: ordersErrored,
    error: ordersError,
    refetch: refetchOrders,
  } = useQuery({
    queryKey: ["business", "orders", ""],
    queryFn: () => orderApi.businessOrders(),
  });
  const {
    data: reservations,
    isLoading: loadingReservations,
    isError: reservationsErrored,
    error: reservationsError,
    refetch: refetchReservations,
  } = useQuery({
    queryKey: ["business", "reservations", ""],
    queryFn: () => reservationApi.businessReservations(),
  });

  const isLoading = loadingBusiness || loadingOrders || loadingReservations;
  const isError = businessErrored || ordersErrored || reservationsErrored;
  const firstError = businessError ?? ordersError ?? reservationsError;
  const refetchAll = () => {
    refetchBusiness();
    refetchOrders();
    refetchReservations();
  };

  const pendingOrders = (orders ?? []).filter((o) => o.status === "PENDING");
  const activeOrders = (orders ?? []).filter((o) => ACTIVE_ORDER_STATUSES.has(o.status));
  const { orderCount, reservationCount, completedRevenue, averageOrderValue } = getLast30DayMetrics(orders ?? [], reservations ?? []);
  const today = localDateKey(new Date());
  const todayReservations = (reservations ?? [])
    .filter((r) => r.date === today && (r.status === "PENDING" || r.status === "CONFIRMED"))
    .sort((a, b) => a.time.localeCompare(b.time));

  const needsAttention: Order[] = [...pendingOrders, ...activeOrders]
    .sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))
    .slice(0, 5);

  return (
    <DashboardShell
      navItems={ownerNavItems}
      title="داشبورد"
      actions={business && <BusinessStatusBadge status={business.status} />}
      topBar={
        <div className="owner-topbar">
          <div className="owner-greeting">
            <h2>سلام کافه‌دار عزیز 👋</h2>
            <p>خوش آمدید، امروز چه برنامه‌ای دارید؟</p>
          </div>
          <div className="owner-topbar-right">
            <span className="admin-topbar-period">۳۰ روز گذشته</span>
            <Link to="/business/reviews" className="notif-bell" aria-label="نظرات">
              <ChatIcon size={18} />
            </Link>
          </div>
        </div>
      }
    >
      {isLoading ? (
        <PageSpinner />
      ) : isError ? (
        <ErrorState error={firstError} onRetry={refetchAll} title="داشبورد لود نشد" />
      ) : (
        <>
          {business && (
            <div className="branch-card">
              {business.coverImageUrl ? (
                <img src={resolveMediaUrl(business.coverImageUrl) ?? undefined} alt={business.name} className="branch-card-image" />
              ) : (
                <span className="branch-card-icon">
                  <StoreIcon size={24} />
                </span>
              )}
              <div className="branch-card-meta">
                <span className="branch-card-name">{business.name}</span>
                <span className="branch-card-address">{business.address ?? "آدرسی ثبت نشده"}</span>
              </div>
            </div>
          )}

          <div className="stat-grid">
            <KpiTile
              icon={<CalendarCheckIcon size={20} />}
              label="رزروها"
              value={reservationCount}
              accent="ember"
            />
            <KpiTile
              icon={<ReceiptIcon size={20} />}
              label="تعداد سفارش"
              value={orderCount}
              accent="pistachio"
            />
            <KpiTile
              icon={<WalletIcon size={20} />}
              label="میزان فروش"
              value={formatToman(completedRevenue)}
              accent="violet"
            />
          </div>

          <div className="owner-quick-actions">
            {QUICK_ACTIONS.map((action) => (
              <Link key={action.to} to={action.to} className="owner-quick-action-btn">
                <span className={`owner-quick-action-icon ${action.accent !== "ember" ? `owner-quick-action-icon-${action.accent}` : ""}`}>
                  {action.icon}
                </span>
                {action.label}
              </Link>
            ))}
          </div>

          <div className="owner-promo-banner">
            <div className="owner-promo-banner-text">
              <h3>با تبلیغات بیشتر دیده شوید</h3>
              <p>با کمپین‌های هدفمند، مشتری‌های جدید جذب کنید</p>
            </div>
            <button
              type="button"
              className="owner-promo-banner-cta"
              onClick={() => notify("ابزارهای تبلیغاتی — این قابلیت بعد از نسخه‌های اولیه اضافه می‌شه.")}
            >
              مشاهده ابزارهای تبلیغاتی
            </button>
          </div>

          <div className="admin-grid">
            <div className="admin-grid-col" style={{ gridColumn: "span 2" }}>
              <Panel
                title="سفارش‌های نیازمند رسیدگی"
                icon={<ReceiptIcon size={17} />}
                action={<Link to="/business/orders">مشاهده همه</Link>}
              >
                {needsAttention.length === 0 ? (
                  <EmptyState title="فعلاً سفارش بازی برای رسیدگی نیست" />
                ) : (
                  <div className="list-group">
                    {needsAttention.map((order) => (
                      <div key={order.id} className="list-row">
                        <div className="list-row-main">
                          <span className="list-row-title">سفارش #{order.id}</span>
                          <span className="list-row-sub">
                            {formatDateTime(order.createdAt)} · {formatToman(order.totalAmount)}
                          </span>
                        </div>
                        <OrderStatusBadge status={order.status} />
                      </div>
                    ))}
                  </div>
                )}
              </Panel>

            </div>

            <div className="admin-grid-col">
              <Panel
                title="رزروهای امروز"
                icon={<CalendarCheckIcon size={17} />}
                action={<Link to="/business/reservations">مشاهده همه</Link>}
              >
                {todayReservations.length === 0 ? (
                  <EmptyState title="برای امروز رزروی ثبت نشده" />
                ) : (
                  <div className="list-group">
                    {todayReservations.map((r: Reservation) => (
                      <div key={r.id} className="list-row">
                        <div className="list-row-main">
                          <span className="list-row-title">{formatTime(r.time)}</span>
                          <span className="list-row-sub">{r.guestCount} نفر</span>
                        </div>
                        <ReservationStatusBadge status={r.status} />
                      </div>
                    ))}
                  </div>
                )}
              </Panel>

            </div>
          </div>

          <div style={{ marginTop: 20 }}>
            <Panel title="خلاصه عملکرد ۳۰ روزه" icon={<ChartIcon size={17} />}>
              <div className="summary-grid" style={{ marginTop: 4 }}>
                <div className="summary-tile">
                  <div className="summary-value">{formatToman(Math.round(averageOrderValue))}</div>
                  <div className="summary-label">میانگین ارزش سفارش</div>
                </div>
              </div>
            </Panel>
          </div>
        </>
      )}
    </DashboardShell>
  );
}

function KpiTile({
  icon,
  label,
  value,
  accent,
}: {
  icon: ReactNode;
  label: string;
  value: number | string;
  accent: Accent;
}) {
  const isNumber = typeof value === "number";
  return (
    <div className="stat-tile">
      <span className={`stat-tile-icon ${accent !== "ember" ? `stat-tile-icon-${accent}` : ""}`}>{icon}</span>
      <span className={`stat-value${isNumber ? "" : " stat-value-text"}`}>
        {isNumber ? new Intl.NumberFormat("fa-IR").format(value) : value}
      </span>
      <span className="stat-label">{label}</span>
    </div>
  );
}
