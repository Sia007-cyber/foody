import type { ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { businessApi } from "../businesses/businessApi";
import { orderApi } from "../orders/orderApi";
import { reservationApi } from "../reservations/reservationApi";
import { DashboardShell } from "../../components/DashboardShell";
import { Panel, PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { BusinessStatusBadge, OrderStatusBadge, ReservationStatusBadge } from "../../components/Badge";
import { formatDateTime, formatTime, formatToman } from "../../lib/format";
import { ownerNavItems } from "./ownerNav";
import { ClockIcon, ReceiptIcon, CalendarCheckIcon, WalletIcon } from "../../components/icons";
import type { Order, Reservation } from "../../types/api";

const ACTIVE_ORDER_STATUSES = new Set(["ACCEPTED", "PREPARING", "READY"]);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

export function OwnerDashboardPage() {
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
  const completedRevenue = (orders ?? [])
    .filter((o) => o.status === "COMPLETED")
    .reduce((sum, o) => sum + Number(o.totalAmount), 0);
  const today = todayIso();
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
    >
      {isLoading ? (
        <PageSpinner />
      ) : isError ? (
        <ErrorState error={firstError} onRetry={refetchAll} title="داشبورد لود نشد" />
      ) : (
        <>
          <div className="stat-grid">
            <StatTile icon={<ClockIcon size={20} />} label="سفارش‌های در انتظار" value={pendingOrders.length} />
            <StatTile icon={<ReceiptIcon size={20} />} label="سفارش‌های در حال انجام" value={activeOrders.length} />
            <StatTile
              icon={<CalendarCheckIcon size={20} />}
              label="رزروهای امروز"
              value={todayReservations.length}
            />
            <StatTile
              icon={<WalletIcon size={20} />}
              label="درآمد سفارش‌های تکمیل‌شده"
              value={formatToman(completedRevenue)}
            />
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
        </>
      )}
    </DashboardShell>
  );
}

function StatTile({ icon, label, value }: { icon: ReactNode; label: string; value: number | string }) {
  const isNumber = typeof value === "number";
  return (
    <div className="stat-tile">
      <span className="stat-tile-icon">{icon}</span>
      <span className={`stat-value${isNumber ? "" : " stat-value-text"}`}>
        {isNumber ? new Intl.NumberFormat("fa-IR").format(value) : value}
      </span>
      <span className="stat-label">{label}</span>
    </div>
  );
}
