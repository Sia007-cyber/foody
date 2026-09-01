import type { ReactNode } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { NotificationBell } from "../features/notifications/NotificationBell";
import { Button } from "./Button";

export interface DashboardNavItem {
  to: string;
  label: string;
  icon?: ReactNode;
}

export function DashboardShell({
  navItems,
  title,
  actions,
  topBar,
  children,
}: {
  navItems: DashboardNavItem[];
  title: string;
  actions?: ReactNode;
  /** Optional rich header row (notifications, date range, avatar) shown above the title. */
  topBar?: ReactNode;
  children: ReactNode;
}) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="dashboard">
      <aside className="dashboard-sidebar">
        <NavLink to="/" className="brand" style={{ paddingInline: 14 }}>
          فودی<span className="brand-dot">.</span>
        </NavLink>
        <nav className="dashboard-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end
              className={({ isActive }) => `dashboard-nav-link ${isActive ? "active" : ""}`}
            >
              {item.icon}
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div style={{ marginTop: "auto" }}>
          <Button variant="ghost" size="sm" onClick={() => logout().then(() => navigate("/"))}>
            خروج
          </Button>
        </div>
      </aside>
      <main className="dashboard-main">
        {topBar}
        <div className="dashboard-header">
          <h1 className="dashboard-title">{title}</h1>
          <div className="dashboard-header-actions">
            {actions}
            <NotificationBell />
          </div>
        </div>
        {children}
      </main>
    </div>
  );
}
