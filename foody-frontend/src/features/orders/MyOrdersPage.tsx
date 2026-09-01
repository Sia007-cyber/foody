import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { orderApi } from "./orderApi";
import { OrderStatusBadge } from "../../components/Badge";
import { PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { formatDateTime, formatToman } from "../../lib/format";

export function MyOrdersPage() {
  const {
    data: orders,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["orders", "my"],
    queryFn: orderApi.myOrders,
  });

  if (isLoading) return <PageSpinner />;

  return (
    <div className="container" style={{ paddingBlock: 40 }}>
      <h1 style={{ fontSize: 24, fontWeight: 800, marginBottom: 24 }}>سفارش‌های من</h1>

      {isError ? (
        <ErrorState error={error} onRetry={() => refetch()} title="سفارش‌ها لود نشدن" />
      ) : !orders || orders.length === 0 ? (
        <EmptyState title="هنوز سفارشی ثبت نکردی" />
      ) : (
        <div className="list-group">
          {orders.map((order) => (
            <Link key={order.id} to={`/orders/${order.id}`} className="list-row">
              <div className="list-row-main">
                <span className="list-row-title">سفارش #{order.id}</span>
                <span className="list-row-sub">
                  {formatDateTime(order.createdAt)} · {formatToman(order.totalAmount)}
                </span>
              </div>
              <OrderStatusBadge status={order.status} />
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
