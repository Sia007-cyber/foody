import { useQuery } from "@tanstack/react-query";
import { adminApi } from "./adminApi";
import { DashboardShell } from "../../components/DashboardShell";
import { PageSpinner } from "../../components/Controls";
import { adminNavItems } from "./adminNav";

export function AdminDashboardPage() {
  const { data: summary, isLoading } = useQuery({
    queryKey: ["admin", "dashboard", "summary"],
    queryFn: adminApi.dashboardSummary,
  });

  return (
    <DashboardShell navItems={adminNavItems} title="داشبورد">
      {isLoading || !summary ? (
        <PageSpinner />
      ) : (
        <div className="stat-grid">
          <StatTile label="کل کاربران" value={summary.totalUsers} />
          <StatTile label="کسب‌وکارهای فعال" value={summary.activeBusinesses} />
          <StatTile label="کل سفارش‌ها" value={summary.totalOrders} />
          <StatTile label="کل رزروها" value={summary.totalReservations} />
        </div>
      )}
    </DashboardShell>
  );
}

function StatTile({ label, value }: { label: string; value: number }) {
  return (
    <div className="stat-tile">
      <span className="stat-value">{new Intl.NumberFormat("en-US").format(value)}</span>
      <span className="stat-label">{label}</span>
    </div>
  );
}
