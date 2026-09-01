import type { DashboardNavItem } from "../../components/DashboardShell";
import {
  DashboardIcon,
  UsersIcon,
  StoreIcon,
  ReceiptIcon,
  WalletIcon,
  MegaphoneIcon,
  ChatIcon,
  ShieldIcon,
  ChartIcon,
  SettingsIcon,
} from "../../components/icons";

export const adminNavItems: DashboardNavItem[] = [
  { to: "/admin", label: "داشبورد", icon: <DashboardIcon size={17} /> },
  { to: "/admin/users", label: "کاربران", icon: <UsersIcon size={17} /> },
  { to: "/admin/businesses", label: "کافه‌ها", icon: <StoreIcon size={17} /> },
  { to: "/admin/orders", label: "سفارش‌ها", icon: <ReceiptIcon size={17} /> },
  { to: "/admin/transactions", label: "تراکنش‌ها", icon: <WalletIcon size={17} /> },
  { to: "/admin/ads", label: "تبلیغات", icon: <MegaphoneIcon size={17} /> },
  { to: "/admin/reviews", label: "نظرات و بازخوردها", icon: <ChatIcon size={17} /> },
  { to: "/admin/violations", label: "گزارشات تخلف", icon: <ShieldIcon size={17} /> },
  { to: "/admin/reports", label: "گزارش‌ها", icon: <ChartIcon size={17} /> },
  { to: "/admin/settings", label: "تنظیمات", icon: <SettingsIcon size={17} /> },
];
