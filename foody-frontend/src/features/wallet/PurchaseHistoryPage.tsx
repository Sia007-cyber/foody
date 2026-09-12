import { useQuery } from "@tanstack/react-query";
import { EmptyState, ErrorState, PageSpinner } from "../../components/Controls";
import { formatDateTime, formatToman } from "../../lib/format";
import { walletApi } from "./walletApi";
import "./wallet.css";

export function PurchaseHistoryPage() {
  const query = useQuery({ queryKey: ["wallet", "purchase-history"], queryFn: walletApi.getPurchaseHistory });
  if (query.isLoading) return <PageSpinner />;
  if (query.isError) return <ErrorState title="تاریخچه خرید لود نشد" error={query.error} onRetry={() => query.refetch()} />;
  return <main className="container wallet-page"><header className="wallet-section-heading"><div><span className="wallet-section-eyebrow">خریدهای حضوری</span><h1>تاریخچه خرید</h1></div></header>
    {!query.data?.length ? <EmptyState title="هنوز خرید تکمیل‌شده‌ای نداری" /> : <div className="wallet-history-groups">{query.data.map((purchase) => <article className="wallet-history-group" key={purchase.id}>
      <h2>{purchase.businessName}</h2><span className="wallet-tx-date">{formatDateTime(purchase.completedAt)}</span>
      <ul className="wallet-tx-list">{purchase.items.map((item, index) => <li className="wallet-tx-row" key={`${purchase.id}-${index}`}><div className="wallet-tx-main"><strong>{item.productName}</strong><span>{item.quantity} × {formatToman(item.unitPrice)}</span></div><strong>{formatToman(item.lineTotal)}</strong></li>)}</ul>
      <p className="wallet-request-amount">مجموع: {formatToman(purchase.totalAmount)}</p>
    </article>)}</div>}
  </main>;
}
