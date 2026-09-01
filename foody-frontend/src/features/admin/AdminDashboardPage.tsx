import type { ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { adminApi } from "./adminApi";
import { DashboardShell } from "../../components/DashboardShell";
import { PageSpinner, Panel, ErrorState } from "../../components/Controls";
import { useAuth } from "../auth/AuthContext";
import { adminNavItems } from "./adminNav";
import {
  UsersIcon,
  StoreIcon,
  ReceiptIcon,
  CalendarCheckIcon,
  BellIcon,
  ChatIcon,
  ActivityIcon,
  ShieldIcon,
  MegaphoneIcon,
  ChevronStartIcon,
  ClockIcon,
  CheckCircleIcon,
  StarIcon,
} from "../../components/icons";

const AD_TOOLS = [
  { icon: <StoreIcon size={18} />, title: "تبلیغ در صفحه اصلی", subtitle: "نمایش تبلیغات در صفحه اصلی اپلیکیشن" },
  { icon: <BellIcon size={18} />, title: "پوش نوتیفیکیشن", subtitle: "ارسال پیام تبلیغاتی به کاربران" },
  { icon: <ActivityIcon size={18} />, title: "ویژه شدن در فهرست", subtitle: "نمایش ویژه‌ی کافه در نتیجه‌ی جست‌وجو" },
  { icon: <ChatIcon size={18} />, title: "تبلیغ در دسته‌بندی‌ها", subtitle: "نمایش در دسته‌بندی‌های منتخب" },
  { icon: <MegaphoneIcon size={18} />, title: "کمپین اختصاصی", subtitle: "طراحی کمپین تبلیغاتی برای کافه" },
];

/**
 * TODO: replace with real data once the backend exposes activity/review/violation
 * endpoints (not in the phase-1 admin API yet — only /admin/dashboard/summary and
 * /admin/businesses exist so far). Kept here as clearly-labeled placeholder content
 * so the layout/UX is real while the data source catches up.
 */
const RECENT_ACTIVITY: { text: string; time: string; icon: ReactNode; tone: "ok" | "danger" | "pending" | "ember" }[] = [
  { text: "«کافه رها» به‌عنوان کسب‌وکار جدید تایید شد", time: "۵ دقیقه پیش", icon: <CheckCircleIcon size={15} />, tone: "ok" },
  { text: "سفارش شماره ۴۵۲۸۹ با تاخیر مواجه شد", time: "۲۲ دقیقه پیش", icon: <ShieldIcon size={15} />, tone: "danger" },
  { text: "کاربر جدید «علی محمدی» ثبت‌نام کرد", time: "۱ ساعت پیش", icon: <UsersIcon size={15} />, tone: "ember" },
  { text: "درخواست تایید «فست‌فود میدان» ثبت شد", time: "۳ ساعت پیش", icon: <ClockIcon size={15} />, tone: "pending" },
];

const RECENT_REVIEWS = [
  { name: "سارا احمدی", biz: "کافه رها", text: "غذا خیلی عالی بود، همیشه دوباره سفارش می‌دم.", rating: 5, time: "۱۰ دقیقه پیش" },
  { name: "علی رضایی", biz: "فست‌فود میدان", text: "کیفیت قهوه متوسط بود ولی سرویس خوب بود.", rating: 3, time: "۳۵ دقیقه پیش" },
  { name: "مهسا کریمی", biz: "کافه آرام", text: "فضای کافه دلنشین و دنج بود.", rating: 4, time: "۱ ساعت پیش" },
];

const OPEN_VIOLATIONS: { biz: string; issue: string; time: string; tone: "danger" | "pending" | "suspended" }[] = [
  { biz: "کافه ونیز", issue: "عدم رعایت کیفیت ثبت‌شده در منو", time: "۲۰ دقیقه پیش", tone: "danger" },
  { biz: "کافه انریوم", issue: "برخورد نامناسب پرسنل", time: "۱ ساعت پیش", tone: "pending" },
  { biz: "کافه رها", issue: "تاخیر مکرر در تحویل سفارش", time: "۳ ساعت پیش", tone: "pending" },
];

export function AdminDashboardPage() {
  const { user } = useAuth();
  const {
    data: summary,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["admin", "dashboard", "summary"],
    queryFn: adminApi.dashboardSummary,
  });

  return (
    <DashboardShell
      navItems={adminNavItems}
      title="داشبورد"
      topBar={
        <div className="admin-topbar">
          <span className="admin-topbar-period">۳۰ روز گذشته</span>
          <div className="admin-topbar-right">
            <div className="admin-user">
              <span className="admin-user-avatar">{(user?.fullName ?? "؟").slice(0, 1)}</span>
              <div className="admin-user-meta">
                <span className="admin-user-name">{user?.fullName ?? "مدیر سیستم"}</span>
                <span className="admin-user-role">خوش آمدید</span>
              </div>
            </div>
          </div>
        </div>
      }
    >
      {isLoading ? (
        <PageSpinner />
      ) : isError || !summary ? (
        <ErrorState error={error} onRetry={() => refetch()} title="داشبورد لود نشد" />
      ) : (
        <>
          <div className="stat-grid">
            <StatTile icon={<UsersIcon size={20} />} label="کل کاربران" value={summary.totalUsers} />
            <StatTile icon={<StoreIcon size={20} />} label="کافه‌های فعال" value={summary.activeBusinesses} />
            <StatTile icon={<ReceiptIcon size={20} />} label="سفارش‌های ثبت‌شده" value={summary.totalOrders} />
            <StatTile icon={<CalendarCheckIcon size={20} />} label="رزروهای ثبت‌شده" value={summary.totalReservations} />
          </div>

          <div className="admin-grid">
            <div className="admin-grid-col">
              <Panel title="آخرین نظرات کاربران" icon={<ChatIcon size={17} />}>
                <ul className="review-list">
                  {RECENT_REVIEWS.map((r) => (
                    <li key={r.name + r.time} className="review-row">
                      <div className="review-row-head">
                        <span>{r.name}</span>
                        <span className="review-rating">
                          <StarIcon size={13} />
                          {new Intl.NumberFormat("fa-IR").format(r.rating)}
                        </span>
                      </div>
                      <p className="review-row-sub">
                        {r.biz} · {r.time}
                      </p>
                      <p className="review-row-text">{r.text}</p>
                    </li>
                  ))}
                </ul>
              </Panel>

              <Panel title="گزارش‌های تخلف باز" icon={<ShieldIcon size={17} />}>
                <ul className="violation-list">
                  {OPEN_VIOLATIONS.map((v) => (
                    <li key={v.biz + v.time} className="violation-row">
                      <div className="violation-meta">
                        <span className="violation-biz">{v.biz}</span>
                        <span className="violation-issue">{v.issue}</span>
                        <span className="violation-time">{v.time}</span>
                      </div>
                      <span className={`violation-badge tone-${v.tone}`}>
                        {v.tone === "danger" ? "بحرانی" : v.tone === "suspended" ? "تعلیق‌شده" : "در حال بررسی"}
                      </span>
                    </li>
                  ))}
                </ul>
              </Panel>
            </div>

            <div className="admin-grid-col">
              <Panel title="آخرین فعالیت‌ها" icon={<ActivityIcon size={17} />}>
                <ul className="activity-list">
                  {RECENT_ACTIVITY.map((a) => (
                    <li key={a.text} className="activity-row">
                      <span className={`activity-icon tone-${a.tone}`}>{a.icon}</span>
                      <span className="activity-meta">
                        <span className="activity-text">{a.text}</span>
                        <span className="activity-time">{a.time}</span>
                      </span>
                    </li>
                  ))}
                </ul>
              </Panel>
            </div>

            <div className="admin-grid-col">
              <Panel title="ابزارهای تبلیغاتی" icon={<MegaphoneIcon size={17} />}>
                <p className="panel-subnote">این ابزارها هنوز فعال نیستند و به‌زودی اضافه خواهند شد.</p>
                <ul className="ad-tool-list">
                  {AD_TOOLS.map((tool) => (
                    <li key={tool.title} className="ad-tool-row" aria-disabled="true">
                      <span className="ad-tool-icon">{tool.icon}</span>
                      <div className="ad-tool-meta">
                        <span className="ad-tool-title">{tool.title}</span>
                        <span className="ad-tool-subtitle">{tool.subtitle}</span>
                      </div>
                      <span className="ad-tool-soon">به‌زودی</span>
                      <ChevronStartIcon size={16} className="ad-tool-chevron" />
                    </li>
                  ))}
                </ul>
              </Panel>
            </div>
          </div>

          <p className="admin-more-link">
            برای مدیریت وضعیت کافه‌ها (تایید/رد/تعلیق) به{" "}
            <Link to="/admin/businesses">صفحه‌ی کافه‌ها</Link> برو.
          </p>
        </>
      )}
    </DashboardShell>
  );
}

function StatTile({ icon, label, value }: { icon: ReactNode; label: string; value: number }) {
  return (
    <div className="stat-tile">
      <span className="stat-tile-icon">{icon}</span>
      <span className="stat-value">{new Intl.NumberFormat("fa-IR").format(value)}</span>
      <span className="stat-label">{label}</span>
    </div>
  );
}
