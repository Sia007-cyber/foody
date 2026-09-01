import type { ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { adminApi } from "./adminApi";
import { DashboardShell } from "../../components/DashboardShell";
import { PageSpinner, ComingSoonPanel, Panel } from "../../components/Controls";
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
} from "../../components/icons";

const AD_TOOLS = [
  { icon: <StoreIcon size={18} />, title: "تبلیغ در صفحه اصلی", subtitle: "نمایش تبلیغات در صفحه اصلی اپلیکیشن" },
  { icon: <BellIcon size={18} />, title: "پوش نوتیفیکیشن", subtitle: "ارسال پیام تبلیغاتی به کاربران" },
  { icon: <ActivityIcon size={18} />, title: "ویژه شدن در فهرست", subtitle: "نمایش ویژه‌ی کافه در نتیجه‌ی جست‌وجو" },
  { icon: <ChatIcon size={18} />, title: "تبلیغ در دسته‌بندی‌ها", subtitle: "نمایش در دسته‌بندی‌های منتخب" },
  { icon: <MegaphoneIcon size={18} />, title: "کمپین اختصاصی", subtitle: "طراحی کمپین تبلیغاتی برای کافه" },
];

export function AdminDashboardPage() {
  const { user } = useAuth();
  const { data: summary, isLoading } = useQuery({
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
            <button type="button" className="admin-bell" aria-label="اعلان‌ها (به‌زودی)" title="اعلان‌ها — به‌زودی">
              <BellIcon size={19} />
            </button>
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
      {isLoading || !summary ? (
        <PageSpinner />
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
              <ComingSoonPanel
                title="پاسخ به نظرات کاربران"
                icon={<ChatIcon size={17} />}
                note="بخش نظرات و پاسخ‌دهی به کاربران هنوز ساخته نشده و به‌زودی اضافه خواهد شد."
              />
              <ComingSoonPanel
                title="بررسی گزارشات تخلف"
                icon={<ShieldIcon size={17} />}
                note="ثبت و بررسی گزارش‌های تخلف کاربران/کافه‌ها هنوز ساخته نشده و به‌زودی اضافه خواهد شد."
              />
            </div>

            <div className="admin-grid-col">
              <ComingSoonPanel
                title="آخرین فعالیت‌ها"
                icon={<ActivityIcon size={17} />}
                note="فید فعالیت‌های زنده‌ی پلتفرم (ثبت کاربر جدید، سفارش جدید و غیره) هنوز ساخته نشده و به‌زودی اضافه خواهد شد."
              />
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
