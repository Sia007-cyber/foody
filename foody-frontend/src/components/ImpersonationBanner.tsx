import { useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { Button } from "./Button";

export function ImpersonationBanner() {
  const { impersonation, exitImpersonation } = useAuth();
  const navigate = useNavigate();
  if (!impersonation) return null;
  return (
    <aside className="impersonation-banner" role="status" aria-live="polite">
      <span><strong>حالت پشتیبانی:</strong> در حال فعالیت به‌جای {impersonation.targetName}</span>
      <Button size="sm" variant="secondary" onClick={() => exitImpersonation().then(() => navigate("/admin/users"))}>
        خروج از حالت پشتیبانی
      </Button>
    </aside>
  );
}
