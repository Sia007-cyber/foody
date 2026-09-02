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
import { ownerNavItems } from "./ownerNav";
import {
  ClockIcon,
  ReceiptIcon,
  CalendarCheckIcon,
  WalletIcon,
  StoreIcon,
  EyeOpenIcon,
  TrendUpIcon,
  TrendDownIcon,
  MegaphoneIcon,
  ChatIcon,
  MenuBookIcon,
  ChartIcon,
  StarIcon,
  UserPlusIcon,
  CheckCircleIcon,
} from "../../components/icons";
import type { Order, Reservation } from "../../types/api";

const ACTIVE_ORDER_STATUSES = new Set(["ACCEPTED", "PREPARING", "READY"]);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

// ---------------------------------------------------------------------------
// بخش‌های زیر (بازدیدکنندگان، درصد تغییرات نسبت به دوره قبل، نظرات مشتریان،
// «آمار تأثیر اپلیکیشن» و بخش‌هایی از خلاصه ۳۰ روزه) هنوز به بک‌اند وصل
// نیستن — چون هیچ endpoint ای برای visit-tracking، نظرات (reviews) یا
// مقایسه‌ی دوره‌ای فعلاً وجود نداره (ماژول reviews فقط یک اسکلت خالیه).
// عدد رزروها/سفارش‌ها/فروش از orderApi و reservationApi واقعیه. وقتی
// endpoint های واقعی آماده شدن، فقط همین چند ثابت زیر باید با useQuery
// جایگزین بشن؛ ساختار و استایل صفحه ثابت می‌مونه.
// ---------------------------------------------------------------------------
const MOCK_VISITORS = 1284;
const MOCK_TRENDS = {
  reservations: 12,
  visitors: 8,
  orders: 5,
  sales: -3,
};

interface Review {
  name: string;
  rating: number;
  time: string;
  text: string;
}

const MOCK_REVIEWS: Review[] = [
  { name: "سارا احمدی", rating: 5, time: "۱۰ دقیقه پیش", text: "غذا خیلی عالی بود، همیشه دوباره سفارش می‌دم." },
  { name: "علی رضایی", rating: 4, time: "۱ ساعت پیش", text: "کیفیت قهوه خیلی خوب بود، فضا هم دنج بود." },
  { name: "مهسا کریمی", rating: 5, time: "۳ ساعت پیش", text: "سرویس‌دهی سریع و برخورد پرسنل عالی بود." },
];

type Accent = "ember" | "violet" | "pistachio";

const IMPACT_STATS: { icon: ReactNode; value: string; label: string; accent: Accent }[] = [
  { icon: <TrendUpIcon size={18} />, value: "۱۸٪", label: "افزایش فروش نسبت به قبل از فودی", accent: "pistachio" },
  { icon: <UserPlusIcon size={18} />, value: "۶۴", label: "مشتری جدید از طریق فودی", accent: "violet" },
  { icon: <ClockIcon size={18} />, value: "۲۳٪", label: "کاهش زمان خالی میزها", accent: "ember" },
  { icon: <StarIcon size={18} />, value: "۴.۷", label: "میانگین امتیاز شما", accent: "violet" },
];

const QUICK_ACTIONS: { to: string; label: string; icon: ReactNode; accent: Accent }[] = [
  { to: "/business/discounts", label: "ایجاد تخفیف", icon: <MegaphoneIcon size={20} />, accent: "violet" },
  { to: "/business/messages", label: "ارسال پیام", icon: <ChatIcon size={20} />, accent: "ember" },
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
  const completedOrders = (orders ?? []).filter((o) => o.status === "COMPLETED");
  const completedRevenue = completedOrders.reduce((sum, o) => sum + Number(o.totalAmount), 0);
  const avgOrderValue = completedOrders.length > 0 ? completedRevenue / completedOrders.length : 0;
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
      topBar={
        <div className="owner-topbar">
          <div className="owner-greeting">
            <h2>سلام کافه‌دار عزیز 👋</h2>
            <p>خوش آمدید، امروز چه برنامه‌ای دارید؟</p>
          </div>
          <div className="owner-topbar-right">
            <span className="admin-topbar-period">۳۰ روز گذشته</span>
            <Link to="/business/messages" className="notif-bell" aria-label="پیام‌ها">
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
                <img src={business.coverImageUrl} alt={business.name} className="branch-card-image" />
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
              value={(reservations ?? []).length}
              trend={MOCK_TRENDS.reservations}
              accent="ember"
            />
            <KpiTile
              icon={<EyeOpenIcon size={20} />}
              label="بازدیدکنندگان"
              value={MOCK_VISITORS}
              trend={MOCK_TRENDS.visitors}
              accent="violet"
            />
            <KpiTile
              icon={<ReceiptIcon size={20} />}
              label="تعداد سفارش"
              value={(orders ?? []).length}
              trend={MOCK_TRENDS.orders}
              accent="pistachio"
            />
            <KpiTile
              icon={<WalletIcon size={20} />}
              label="میزان فروش"
              value={formatToman(completedRevenue)}
              trend={MOCK_TRENDS.sales}
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

              <Panel title="نظرات مشتریان" icon={<ChatIcon size={17} />} action={<Link to="/business/reports">مشاهده همه</Link>}>
                <ul className="review-list">
                  {MOCK_REVIEWS.map((r) => (
                    <li key={r.name + r.time} className="review-row">
                      <div className="review-row-owner">
                        <span className="review-avatar">{r.name.slice(0, 1)}</span>
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <div className="review-row-head">
                            <span>{r.name}</span>
                            <span className="review-rating">
                              <StarIcon size={13} />
                              {new Intl.NumberFormat("fa-IR").format(r.rating)}
                            </span>
                          </div>
                          <p className="review-row-sub">{r.time}</p>
                          <p className="review-row-text">{r.text}</p>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
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

              <Panel title="آمار تأثیر اپلیکیشن برای شما" icon={<CheckCircleIcon size={17} />}>
                <div className="impact-grid" style={{ gridTemplateColumns: "1fr" }}>
                  {IMPACT_STATS.map((s) => (
                    <div key={s.label} className="impact-tile">
                      <span className={`impact-icon impact-icon-${s.accent}`}>{s.icon}</span>
                      <div>
                        <div className="impact-value">{s.value}</div>
                        <div className="impact-label">{s.label}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </Panel>
            </div>
          </div>

          <div style={{ marginTop: 20 }}>
            <Panel title="خلاصه عملکرد ۳۰ روزه" icon={<ChartIcon size={17} />}>
              <div className="summary-grid" style={{ marginTop: 4 }}>
                <div className="summary-tile">
                  <div className="summary-value">۴۲٪</div>
                  <div className="summary-label">نرخ بازگشت مشتری</div>
                </div>
                <div className="summary-tile">
                  <div className="summary-value">{formatToman(Math.round(avgOrderValue))}</div>
                  <div className="summary-label">میانگین ارزش سفارش</div>
                </div>
                <div className="summary-tile">
                  <div className="summary-value">۶۴</div>
                  <div className="summary-label">مشتریان جدید</div>
                </div>
                <div className="summary-tile">
                  <div className="summary-value">۴.۷</div>
                  <div className="summary-label">میانگین امتیاز</div>
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
  trend,
  accent,
}: {
  icon: ReactNode;
  label: string;
  value: number | string;
  trend: number;
  accent: Accent;
}) {
  const isNumber = typeof value === "number";
  const isUp = trend >= 0;
  return (
    <div className="stat-tile">
      <span className={`kpi-trend ${isUp ? "up" : "down"}`}>
        {isUp ? <TrendUpIcon size={12} /> : <TrendDownIcon size={12} />}
        {new Intl.NumberFormat("fa-IR", { signDisplay: "never" }).format(Math.abs(trend))}٪
      </span>
      <span className={`stat-tile-icon ${accent !== "ember" ? `stat-tile-icon-${accent}` : ""}`}>{icon}</span>
      <span className={`stat-value${isNumber ? "" : " stat-value-text"}`}>
        {isNumber ? new Intl.NumberFormat("fa-IR").format(value) : value}
      </span>
      <span className="stat-label">{label}</span>
    </div>
  );
}
