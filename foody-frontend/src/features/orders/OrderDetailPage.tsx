import { useState } from "react";
import { useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { orderApi } from "./orderApi";
import { OrderStatusBadge } from "../../components/Badge";
import { PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { Button } from "../../components/Button";
import { ConfirmDialog, useToast, errorMessage } from "../../components/Feedback";
import { formatDateTime, formatToman } from "../../lib/format";

const fulfillmentLabel: Record<string, string> = {
  PICKUP: "دریافت حضوری",
  DELIVERY: "ارسال",
  DINE_IN: "سرو در محل",
};

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [confirmingCancel, setConfirmingCancel] = useState(false);

  const {
    data: order,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["orders", orderId],
    queryFn: () => orderApi.getById(orderId),
    enabled: Number.isFinite(orderId),
  });

  const cancelMutation = useMutation({
    mutationFn: () => orderApi.cancel(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      notify("سفارش لغو شد", "ok");
      setConfirmingCancel(false);
    },
    onError: (err) => {
      notify(errorMessage(err), "danger");
      setConfirmingCancel(false);
    },
  });

  if (isLoading) return <PageSpinner />;
  if (isError) return <ErrorState error={error} onRetry={() => refetch()} title="سفارش لود نشد" />;
  if (!order) return <EmptyState title="سفارش پیدا نشد" />;

  return (
    <div className="container" style={{ maxWidth: 560, paddingBlock: 40 }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8 }}>
        <h1 style={{ fontSize: 24, fontWeight: 800 }}>سفارش #{order.id}</h1>
        <OrderStatusBadge status={order.status} />
      </div>
      <p style={{ color: "var(--ink-soft)", fontSize: 14, marginBottom: 24 }}>
        {formatDateTime(order.createdAt)} · {fulfillmentLabel[order.fulfillmentType] ?? order.fulfillmentType}
        {order.deliveryAddress ? ` · ${order.deliveryAddress}` : ""}
      </p>

      <div className="list-group" style={{ marginBottom: 24 }}>
        {order.items.map((item) => (
          <div key={item.productId} className="list-row">
            <div className="list-row-main">
              <span className="list-row-title">
                {item.productName} × {item.quantity}
              </span>
            </div>
            <span>{formatToman(item.subtotal)}</span>
          </div>
        ))}
        <div className="list-row">
          <span className="list-row-title">جمع کل</span>
          <span style={{ fontWeight: 700, color: "var(--ember-deep)" }}>{formatToman(order.totalAmount)}</span>
        </div>
      </div>

      {order.status === "PENDING" && (
        <Button variant="danger" onClick={() => setConfirmingCancel(true)}>
          لغو سفارش
        </Button>
      )}

      {confirmingCancel && (
        <ConfirmDialog
          title="سفارش لغو بشه؟"
          description="این عملیات قابل بازگشت نیست."
          confirmLabel="لغو سفارش"
          danger
          loading={cancelMutation.isPending}
          onConfirm={() => cancelMutation.mutate()}
          onCancel={() => setConfirmingCancel(false)}
        />
      )}
    </div>
  );
}
