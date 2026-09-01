import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { orderApi } from "../orders/orderApi";
import { DashboardShell } from "../../components/DashboardShell";
import { Select } from "../../components/Field";
import { Button } from "../../components/Button";
import { OrderStatusBadge } from "../../components/Badge";
import { PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { useToast, errorMessage } from "../../components/Feedback";
import { formatDateTime, formatToman } from "../../lib/format";
import { ownerNavItems } from "./ownerNav";
import type { OrderStatus } from "../../types/api";

const statusOptions: { value: OrderStatus | ""; label: string }[] = [
  { value: "", label: "همه‌ی وضعیت‌ها" },
  { value: "PENDING", label: "در انتظار" },
  { value: "ACCEPTED", label: "پذیرفته‌شده" },
  { value: "PREPARING", label: "در حال آماده‌سازی" },
  { value: "READY", label: "آماده" },
  { value: "COMPLETED", label: "تکمیل‌شده" },
  { value: "REJECTED", label: "رد‌شده" },
  { value: "CANCELLED", label: "لغو‌شده" },
];

// Mirrors OrderServiceImpl.VALID_BUSINESS_TRANSITIONS exactly.
const nextActions: Partial<Record<OrderStatus, { status: OrderStatus; label: string; danger?: boolean }[]>> = {
  PENDING: [
    { status: "ACCEPTED", label: "پذیرفتن" },
    { status: "REJECTED", label: "رد کردن", danger: true },
  ],
  ACCEPTED: [{ status: "PREPARING", label: "شروع آماده‌سازی" }],
  PREPARING: [{ status: "READY", label: "آماده شد" }],
  READY: [{ status: "COMPLETED", label: "تکمیل شد" }],
};

export function OwnerOrdersPage() {
  const [status, setStatus] = useState<OrderStatus | "">("");
  const queryClient = useQueryClient();
  const { notify } = useToast();

  const {
    data: orders,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["business", "orders", status],
    queryFn: () => orderApi.businessOrders(status || undefined),
  });

  const updateStatus = useMutation({
    mutationFn: ({ id, newStatus }: { id: number; newStatus: OrderStatus }) =>
      orderApi.updateStatus(id, newStatus),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["business", "orders"] });
      notify("وضعیت سفارش به‌روزرسانی شد", "ok");
    },
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  return (
    <DashboardShell
      navItems={ownerNavItems}
      title="سفارش‌ها"
      actions={
        <Select value={status} onChange={(e) => setStatus(e.target.value as OrderStatus | "")}>
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
        <ErrorState error={error} onRetry={() => refetch()} title="سفارش‌ها لود نشدن" />
      ) : !orders || orders.length === 0 ? (
        <EmptyState title="سفارشی پیدا نشد" />
      ) : (
        <div className="list-group">
          {orders.map((order) => (
            <div key={order.id} className="list-row">
              <div className="list-row-main">
                <span className="list-row-title">سفارش #{order.id}</span>
                <span className="list-row-sub">
                  {formatDateTime(order.createdAt)} · {formatToman(order.totalAmount)}
                </span>
              </div>
              <div className="list-row-actions">
                <OrderStatusBadge status={order.status} />
                {(nextActions[order.status] ?? []).map((action) => (
                  <Button
                    key={action.status}
                    size="sm"
                    variant={action.danger ? "danger" : "secondary"}
                    loading={updateStatus.isPending}
                    onClick={() => updateStatus.mutate({ id: order.id, newStatus: action.status })}
                  >
                    {action.label}
                  </Button>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </DashboardShell>
  );
}
