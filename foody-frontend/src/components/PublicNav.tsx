import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { useCart } from "../features/cart/CartContext";
import { NotificationBell } from "../features/notifications/NotificationBell";
import { AvatarMenuButton } from "./AvatarMenuButton";
import { Button } from "./Button";
import { MenuIcon, CloseIcon, LogoutIcon } from "./icons";

export function PublicNav() {
  const { user, logout } = useAuth();
  const { totalItems } = useCart();
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

  const navItems = (
    <>
      {user && (
        <NavLink
          to="/"
          end
          onClick={() => setDrawerOpen(false)}
          className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}
        >
          کشف کسب‌وکارها
        </NavLink>
      )}
      {(user?.role === "CUSTOMER" || user?.role === "BUSINESS_OWNER") && (
        <>
          <NavLink
            to="/orders"
            onClick={() => setDrawerOpen(false)}
            className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}
          >
            سفارش‌های من
          </NavLink>
          <NavLink
            to="/reservations"
            onClick={() => setDrawerOpen(false)}
            className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}
          >
            رزروهای من
          </NavLink>
          <NavLink
            to={user?.role === "BUSINESS_OWNER" ? "/business/wallets" : "/wallet"}
            onClick={() => setDrawerOpen(false)}
            className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}
          >
            {user?.role === "BUSINESS_OWNER" ? "کیف پول مشتری‌ها" : "کیف پول من"}
          </NavLink>
        </>
      )}
      {(user?.role === "CUSTOMER" || user?.role === "BUSINESS_OWNER") && (
        <>
          <NavLink to="/offers" onClick={() => setDrawerOpen(false)} className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
            پیشنهادها
          </NavLink>
          <NavLink to="/offers/my-claims" onClick={() => setDrawerOpen(false)} className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
            دریافت‌های من
          </NavLink>
        </>
      )}
      {user?.role === "BUSINESS_OWNER" && (
        <NavLink
          to="/business"
          onClick={() => setDrawerOpen(false)}
          className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}
        >
          پنل کسب‌وکار
        </NavLink>
      )}
      {user?.role === "ADMIN" && (
        <NavLink
          to="/admin"
          onClick={() => setDrawerOpen(false)}
          className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}
        >
          پنل ادمین
        </NavLink>
      )}
    </>
  );

  return (
    <header className="public-nav">
      <div className="container public-nav-inner">
        <div className="public-nav-start">
          {user && (
            <button
              type="button"
              className="nav-drawer-toggle"
              aria-label="باز کردن منو"
              onClick={() => setDrawerOpen(true)}
            >
              <MenuIcon size={20} />
            </button>
          )}
          <NavLink to="/" className="brand">
            فودی<span className="brand-dot">.</span>
          </NavLink>
        </div>

        <nav className="nav-links">{navItems}</nav>

        <div className="nav-actions">
          {user && <NotificationBell />}
          {user && <AvatarMenuButton />}
          {(user?.role === "CUSTOMER" || user?.role === "BUSINESS_OWNER") && totalItems > 0 && (
            <Button variant="secondary" size="sm" onClick={() => navigate("/checkout")}>
              سبد خرید
              <span className="nav-cart-badge">{totalItems}</span>
            </Button>
          )}
          {user && (
            <button
              type="button"
              className="nav-logout-btn"
              aria-label="خروج"
              title="خروج"
              onClick={() => logout().then(() => navigate("/"))}
            >
              <LogoutIcon size={18} />
            </button>
          )}
        </div>
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
              <nav className="nav-drawer-list">{navItems}</nav>
              {user && (
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
              )}
            </div>
          </div>,
          document.body,
        )}
    </header>
  );
}
