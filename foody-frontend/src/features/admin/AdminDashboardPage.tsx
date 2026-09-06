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
  MegaphoneIcon,
} from "../../components/icons";

type Accent = "ember" | "violet" | "pistachio";

const AD_TOOLS: { icon: ReactNode; title: string; subtitle: string; accent: Accent }[] = [
  { icon: <StoreIcon size={18} />, title: "تبلیغ در صفحه اصلی", subtitle: "نمایش تبلیغات در صفحه اصلی اپلیکیشن", accent: "ember" },
  { icon: <BellIcon size={18} />, title: "پوش نوتیفیکیشن", subtitle: "ارسال پیام تبلیغاتی به کاربران", accent: "violet" },
  { icon: <ActivityIcon size={18} />, title: "ویژه شدن در فهرست", subtitle: "نمایش ویژه‌ی کافه در نتیجه‌ی جست‌وجو", accent: "pistachio" },
  { icon: <ChatIcon size={18} />, title: "تبلیغ در دسته‌بندی‌ها", subtitle: "نمایش در دسته‌بندی‌های منتخب", accent: "violet" },
  { icon: <MegaphoneIcon size={18} />, title: "کمپین اختصاصی", subtitle: "طراحی کمپین تبلیغاتی برای کافه", accent: "ember" },
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
          <span className="admin-topbar-period">تمام دوران</span>
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
            <StatTile icon={<UsersIcon size={20} />} label="کل کاربران" value={summary.totalUsers} accent="ember" />
            <StatTile icon={<StoreIcon size={20} />} label="کافه‌های فعال" value={summary.activeBusinesses} accent="violet" />
            <StatTile icon={<ReceiptIcon size={20} />} label="سفارش‌های ثبت‌شده" value={summary.totalOrders} accent="pistachio" />
            <StatTile icon={<CalendarCheckIcon size={20} />} label="رزروهای ثبت‌شده" value={summary.totalReservations} accent="ember" />
          </div>

          <Panel
            title="ابزارهای تبلیغاتی"
            icon={<MegaphoneIcon size={17} />}
            action={<span className="panel-header-note">به‌زودی</span>}
          >
            <p className="panel-subnote">این ابزارها هنوز فعال نیستند و به‌زودی اضافه خواهند شد.</p>
            <div className="ad-tool-grid">
              {AD_TOOLS.map((tool) => (
                <div key={tool.title} className="ad-tool-card" aria-disabled="true">
                  <span className={`ad-tool-icon ${tool.accent !== "ember" ? `ad-tool-icon-${tool.accent}` : ""}`}>
                    {tool.icon}
                  </span>
                  <div className="ad-tool-card-meta">
                    <span className="ad-tool-title">{tool.title}</span>
                    <span className="ad-tool-subtitle">{tool.subtitle}</span>
                  </div>
                  <span className="ad-tool-soon">به‌زودی</span>
                </div>
              ))}
            </div>
          </Panel>

          <p className="admin-more-link">
            برای مدیریت وضعیت کافه‌ها (تایید/رد/تعلیق) به{" "}
            <Link to="/admin/businesses">صفحه‌ی کافه‌ها</Link> برو.
          </p>
        </>
      )}
    </DashboardShell>
  );
}

function StatTile({
  icon,
  label,
  value,
  accent,
}: {
  icon: ReactNode;
  label: string;
  value: number;
  accent: Accent;
}) {
  return (
    <div className="stat-tile">
      <span className={`stat-tile-icon ${accent !== "ember" ? `stat-tile-icon-${accent}` : ""}`}>{icon}</span>
      <span className="stat-value">{new Intl.NumberFormat("fa-IR").format(value)}</span>
      <span className="stat-label">{label}</span>
    </div>
  );
}
