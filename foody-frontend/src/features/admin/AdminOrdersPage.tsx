import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { adminApi } from "./adminApi";
import { DashboardShell } from "../../components/DashboardShell";
import { Select } from "../../components/Field";
import { OrderStatusBadge } from "../../components/Badge";
import { PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { formatDateTime, formatToman } from "../../lib/format";
import { adminNavItems } from "./adminNav";
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

const fulfillmentLabel: Record<string, string> = {
  PICKUP: "دریافت حضوری",
  DELIVERY: "ارسال",
  DINE_IN: "سرو در محل",
};

// Read-only overview: admin can inspect orders across every business, but only
// the owning business drives the order lifecycle (see OwnerOrdersPage).
export function AdminOrdersPage() {
  const [status, setStatus] = useState<OrderStatus | "">("");
  const [businessId, setBusinessId] = useState<string>("");
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const { data: businesses } = useQuery({
    queryKey: ["admin", "businesses", "all"],
    queryFn: () => adminApi.businesses(),
  });

  const {
    data: orders,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["admin", "orders", status, businessId],
    queryFn: () => adminApi.orders(status || undefined, businessId ? Number(businessId) : undefined),
  });

  return (
    <DashboardShell
      navItems={adminNavItems}
      title="سفارش‌ها"
      actions={
        <div style={{ display: "flex", gap: 8 }}>
          <Select value={businessId} onChange={(e) => setBusinessId(e.target.value)}>
            <option value="">همه‌ی کسب‌وکارها</option>
            {(businesses ?? []).map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </Select>
          <Select value={status} onChange={(e) => setStatus(e.target.value as OrderStatus | "")}>
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
        <ErrorState error={error} onRetry={() => refetch()} title="سفارش‌ها لود نشدن" />
      ) : !orders || orders.length === 0 ? (
        <EmptyState title="سفارشی پیدا نشد" />
      ) : (
        <div className="list-group">
          {orders.map((order) => (
            <div key={order.id}>
              <div
                className="list-row"
                style={{ cursor: "pointer" }}
                onClick={() => setExpandedId(expandedId === order.id ? null : order.id)}
              >
                <div className="list-row-main">
                  <span className="list-row-title">
                    سفارش #{order.id} · {order.businessName}
                  </span>
                  <span className="list-row-sub">
                    {order.customerName} · {fulfillmentLabel[order.fulfillmentType] ?? order.fulfillmentType} ·{" "}
                    {formatDateTime(order.createdAt)}
                  </span>
                </div>
                <div className="list-row-actions">
                  <span style={{ fontWeight: 700 }}>{formatToman(order.totalAmount)}</span>
                  <OrderStatusBadge status={order.status} />
                </div>
              </div>
              {expandedId === order.id && (
                <div className="list-row-detail">
                  {order.customerEmail && (
                    <div className="list-row-detail-line">
                      <span>ایمیل مشتری</span>
                      <span>{order.customerEmail}</span>
                    </div>
                  )}
                  {order.customerPhone && (
                    <div className="list-row-detail-line">
                      <span>تلفن مشتری</span>
                      <span>{order.customerPhone}</span>
                    </div>
                  )}
                  {order.deliveryAddress && (
                    <div className="list-row-detail-line">
                      <span>آدرس ارسال</span>
                      <span>{order.deliveryAddress}</span>
                    </div>
                  )}
                  {order.items.map((item) => (
                    <div key={item.productId} className="list-row-detail-line">
                      <span>
                        {item.productName} × {item.quantity}
                      </span>
                      <span>{formatToman(item.subtotal)}</span>
                    </div>
                  ))}
                  <div className="list-row-detail-line list-row-detail-total">
                    <span>جمع کل</span>
                    <span>{formatToman(order.totalAmount)}</span>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </DashboardShell>
  );
}
