import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "./adminApi";
import { DashboardShell } from "../../components/DashboardShell";
import { Select } from "../../components/Field";
import { Button } from "../../components/Button";
import { UserStatusBadge } from "../../components/Badge";
import { PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { useToast, errorMessage } from "../../components/Feedback";
import { adminNavItems } from "./adminNav";
import type { UserRole, UserStatus } from "../../types/api";

const roleOptions: { value: UserRole | ""; label: string }[] = [
  { value: "", label: "همه‌ی نقش‌ها" },
  { value: "CUSTOMER", label: "مشتری" },
  { value: "BUSINESS_OWNER", label: "صاحب کسب‌وکار" },
  { value: "ADMIN", label: "مدیر" },
];

const statusOptions: { value: UserStatus | ""; label: string }[] = [
  { value: "", label: "همه‌ی وضعیت‌ها" },
  { value: "ACTIVE", label: "فعال" },
  { value: "SUSPENDED", label: "معلق" },
];

const roleLabels: Record<UserRole, string> = {
  CUSTOMER: "مشتری",
  BUSINESS_OWNER: "صاحب کسب‌وکار",
  ADMIN: "مدیر",
};

// Mirrors UserServiceImpl.updateStatus: admin accounts can't be suspended/reactivated
// from this panel (an admin must never be able to lock themself or another admin out).
export function AdminUsersPage() {
  const [role, setRole] = useState<UserRole | "">("");
  const [status, setStatus] = useState<UserStatus | "">("");
  const queryClient = useQueryClient();
  const { notify } = useToast();

  const {
    data: users,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["admin", "users", role, status],
    queryFn: () => adminApi.users(role || undefined, status || undefined),
  });

  const onMutationSettled = (successMessage: string) => ({
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
      notify(successMessage, "ok");
    },
    onError: (err: unknown) => notify(errorMessage(err), "danger"),
  });

  const suspendMutation = useMutation({ mutationFn: adminApi.suspendUser, ...onMutationSettled("کاربر معلق شد") });
  const activateMutation = useMutation({
    mutationFn: adminApi.activateUser,
    ...onMutationSettled("کاربر فعال شد"),
  });

  return (
    <DashboardShell
      navItems={adminNavItems}
      title="کاربران"
      actions={
        <div style={{ display: "flex", gap: 8 }}>
          <Select value={role} onChange={(e) => setRole(e.target.value as UserRole | "")}>
            {roleOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </Select>
          <Select value={status} onChange={(e) => setStatus(e.target.value as UserStatus | "")}>
            {statusOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </Select>
        </div>
      }
    >
      {isLoading ? (
        <PageSpinner />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} title="کاربران لود نشدن" />
      ) : !users || users.length === 0 ? (
        <EmptyState title="کاربری پیدا نشد" />
      ) : (
        <div className="list-group">
          {users.map((u) => (
            <div key={u.id} className="list-row">
              <div className="list-row-main">
                <span className="list-row-title">{u.fullName}</span>
                <span className="list-row-sub">
                  {u.email} · {roleLabels[u.role]}
                  {u.phone ? ` · ${u.phone}` : ""}
                </span>
              </div>
              <div className="list-row-actions">
                <UserStatusBadge status={u.status} />
                {u.role !== "ADMIN" && u.status === "ACTIVE" && (
                  <Button size="sm" variant="danger" onClick={() => suspendMutation.mutate(u.id)}>
                    معلق کردن
                  </Button>
                )}
                {u.role !== "ADMIN" && u.status === "SUSPENDED" && (
                  <Button size="sm" onClick={() => activateMutation.mutate(u.id)}>
                    فعال‌سازی مجدد
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </DashboardShell>
  );
}
