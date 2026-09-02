import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { useCart } from "../features/cart/CartContext";
import { NotificationBell } from "../features/notifications/NotificationBell";
import { Button } from "./Button";

export function PublicNav() {
  const { user, logout } = useAuth();
  const { totalItems } = useCart();
  const navigate = useNavigate();

  return (
    <header className="public-nav">
      <div className="container public-nav-inner">
        <NavLink to="/" className="brand">
          فودی<span className="brand-dot">.</span>
        </NavLink>

        <nav className="nav-links">
          <NavLink to="/" end className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
            کشف کسب‌وکارها
          </NavLink>
          {(user?.role === "CUSTOMER" || user?.role === "BUSINESS_OWNER") && (
            <>
              <NavLink to="/orders" className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
                سفارش‌های من
              </NavLink>
              <NavLink
                to="/reservations"
                className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}
              >
                رزروهای من
              </NavLink>
            </>
          )}
          {user?.role === "BUSINESS_OWNER" && (
            <a href="/business" target="_blank" rel="noopener noreferrer" className="nav-link">
              پنل کسب‌وکار
            </a>
          )}
          {user?.role === "ADMIN" && (
            <NavLink to="/admin" className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
              پنل ادمین
            </NavLink>
          )}
        </nav>

        <div className="nav-actions">
          {user && <NotificationBell />}
          {(user?.role === "CUSTOMER" || user?.role === "BUSINESS_OWNER") && totalItems > 0 && (
            <Button variant="secondary" size="sm" onClick={() => navigate("/checkout")}>
              سبد خرید
              <span className="nav-cart-badge">{totalItems}</span>
            </Button>
          )}
          {user && (
            <Button variant="ghost" size="sm" onClick={() => logout().then(() => navigate("/"))}>
              خروج
            </Button>
          )}
        </div>
      </div>
    </header>
  );
}
