import type { ReactNode } from "react";
import { DashboardShell, type DashboardNavItem } from "../components/DashboardShell";

/**
 * Full placeholder page for sidebar sections that are designed but not implemented yet
 * (no backend module behind them). Keeps the intended navigation/IA visible to
 * stakeholders without faking data or functionality.
 */
export function ComingSoonFeaturePage({
  navItems,
  title,
  description,
  icon,
}: {
  navItems: DashboardNavItem[];
  title: string;
  description: string;
  icon?: ReactNode;
}) {
  return (
    <DashboardShell navItems={navItems} title={title}>
      <div className="coming-soon-page">
        {icon && <div className="coming-soon-page-icon">{icon}</div>}
        <span className="coming-soon-badge">به‌زودی</span>
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
    </DashboardShell>
  );
}
