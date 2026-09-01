import type { DashboardNavItem } from "../../components/DashboardShell";
import { DashboardIcon, StoreIcon, MenuBookIcon, ReceiptIcon, CalendarCheckIcon } from "../../components/icons";

export const ownerNavItems: DashboardNavItem[] = [
  { to: "/business", label: "داشبورد", icon: <DashboardIcon size={17} /> },
  { to: "/business/profile", label: "پروفایل", icon: <StoreIcon size={17} /> },
  { to: "/business/menus", label: "منو و محصولات", icon: <MenuBookIcon size={17} /> },
  { to: "/business/orders", label: "سفارش‌ها", icon: <ReceiptIcon size={17} /> },
  { to: "/business/reservations", label: "رزروها", icon: <CalendarCheckIcon size={17} /> },
];
