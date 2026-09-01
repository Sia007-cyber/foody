import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "./adminApi";
import { DashboardShell } from "../../components/DashboardShell";
import { Select } from "../../components/Field";
import { Button } from "../../components/Button";
import { BusinessStatusBadge } from "../../components/Badge";
import { PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { useToast, errorMessage } from "../../components/Feedback";
import { adminNavItems } from "./adminNav";
import type { BusinessStatus } from "../../types/api";

const statusOptions: { value: BusinessStatus | ""; label: string }[] = [
  { value: "", label: "همه‌ی وضعیت‌ها" },
  { value: "PENDING", label: "در انتظار تایید" },
  { value: "APPROVED", label: "تایید‌شده" },
  { value: "REJECTED", label: "رد‌شده" },
  { value: "SUSPENDED", label: "معلق" },
];

// Mirrors BusinessServiceImpl.VALID_ADMIN_TRANSITIONS exactly:
// PENDING -> APPROVED / REJECTED, APPROVED -> SUSPENDED.
export function AdminBusinessesPage() {
  const [status, setStatus] = useState<BusinessStatus | "">("PENDING");
  const queryClient = useQueryClient();
  const { notify } = useToast();

  const {
    data: businesses,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["admin", "businesses", status],
    queryFn: () => adminApi.businesses(status || undefined),
  });

  const onMutationSettled = (successMessage: string) => ({
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "businesses"] });
      notify(successMessage, "ok");
    },
    onError: (err: unknown) => notify(errorMessage(err), "danger"),
  });

  const approveMutation = useMutation({ mutationFn: adminApi.approve, ...onMutationSettled("کسب‌وکار تایید شد") });
  const rejectMutation = useMutation({ mutationFn: adminApi.reject, ...onMutationSettled("کسب‌وکار رد شد") });
  const suspendMutation = useMutation({ mutationFn: adminApi.suspend, ...onMutationSettled("کسب‌وکار معلق شد") });

  return (
    <DashboardShell
      navItems={adminNavItems}
      title="کسب‌وکارها"
      actions={
        <Select value={status} onChange={(e) => setStatus(e.target.value as BusinessStatus | "")}>
          {statusOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </Select>
      }
    >
      {isLoading ? (
        <PageSpinner />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} title="کسب‌وکارها لود نشدن" />
      ) : !businesses || businesses.length === 0 ? (
        <EmptyState title="کسب‌وکاری پیدا نشد" />
      ) : (
        <div className="list-group">
          {businesses.map((b) => (
            <div key={b.id} className="list-row">
              <div className="list-row-main">
                <span className="list-row-title">{b.name}</span>
                <span className="list-row-sub">
                  {b.businessType} {b.address ? `· ${b.address}` : ""}
                </span>
              </div>
              <div className="list-row-actions">
                <BusinessStatusBadge status={b.status} />
                {b.status === "PENDING" && (
                  <>
                    <Button size="sm" onClick={() => approveMutation.mutate(b.id)}>
                      تایید
                    </Button>
                    <Button size="sm" variant="danger" onClick={() => rejectMutation.mutate(b.id)}>
                      رد کردن
                    </Button>
                  </>
                )}
                {b.status === "APPROVED" && (
                  <Button size="sm" variant="danger" onClick={() => suspendMutation.mutate(b.id)}>
                    معلق کردن
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
