import { type ReactNode, useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { NotificationBell } from "../features/notifications/NotificationBell";
import { AvatarMenuButton } from "./AvatarMenuButton";
import { Button } from "./Button";
import { MenuIcon, CloseIcon, LogoutIcon } from "./icons";

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
  const [drawerOpen, setDrawerOpen] = useState(false);

  // بستن کشو با تغییر مسیر و جلوگیری از اسکرول پس‌زمینه وقتی بازه
  useEffect(() => {
    if (drawerOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [drawerOpen]);

  const navLinks = (
    <>
      {navItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end
          onClick={() => setDrawerOpen(false)}
          className={({ isActive }) => `dashboard-nav-link ${isActive ? "active" : ""}`}
        >
          {item.icon}
          {item.label}
        </NavLink>
      ))}
    </>
  );

  return (
    <div className="dashboard">
      <aside className="dashboard-sidebar">
        <NavLink to="/" className="brand" style={{ paddingInline: 14 }}>
          فودی<span className="brand-dot">.</span>
        </NavLink>
        <nav className="dashboard-nav">{navLinks}</nav>
        <div style={{ marginTop: "auto" }}>
          <Button variant="ghost" size="sm" onClick={() => logout().then(() => navigate("/"))}>
            خروج
          </Button>
        </div>
      </aside>

      <div className="dashboard-mobile-topbar">
        <button
          type="button"
          className="nav-drawer-toggle"
          aria-label="باز کردن منو"
          onClick={() => setDrawerOpen(true)}
        >
          <MenuIcon size={20} />
        </button>
        <NavLink to="/" className="brand">
          فودی<span className="brand-dot">.</span>
        </NavLink>
      </div>

      {drawerOpen &&
        createPortal(
          <div className="nav-drawer-backdrop" onClick={() => setDrawerOpen(false)}>
            <div className="nav-drawer" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
              <div className="nav-drawer-header">
                <span className="brand">
                  فودی<span className="brand-dot">.</span>
                </span>
                <button
                  type="button"
                  className="nav-drawer-close"
                  aria-label="بستن منو"
                  onClick={() => setDrawerOpen(false)}
                >
                  <CloseIcon size={18} />
                </button>
              </div>
              <nav className="nav-drawer-list">{navLinks}</nav>
              <button
                type="button"
                className="nav-drawer-logout"
                onClick={() => {
                  setDrawerOpen(false);
                  logout().then(() => navigate("/"));
                }}
              >
                <LogoutIcon size={17} />
                خروج
              </button>
            </div>
          </div>,
          document.body,
        )}

      <main className="dashboard-main">
        {topBar}
        <div className="dashboard-header">
          <h1 className="dashboard-title">{title}</h1>
          <div className="dashboard-header-actions">
            {actions}
            <NotificationBell />
            <AvatarMenuButton />
          </div>
        </div>
        {children}
      </main>
    </div>
  );
}
