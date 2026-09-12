import { useQuery } from "@tanstack/react-query";
import { DashboardShell } from "../../components/DashboardShell";
import { EmptyState, ErrorState, PageSpinner } from "../../components/Controls";
import { formatDateTime, formatToman } from "../../lib/format";
import { ownerNavItems } from "./ownerNav";
import { ownerWalletApi } from "./ownerWalletApi";

export function OwnerSalesHistoryPage(){const query=useQuery({queryKey:["business","sales-history"],queryFn:ownerWalletApi.getSalesHistory});return <DashboardShell navItems={ownerNavItems} title="تاریخچه فروش">{query.isLoading?<PageSpinner/>:query.isError?<ErrorState title="تاریخچه فروش لود نشد" error={query.error} onRetry={()=>query.refetch()}/>:!query.data?.length?<EmptyState title="هنوز فروش تکمیل‌شده‌ای ثبت نشده"/>:<div className="wallet-history-groups">{query.data.map(sale=><article className="wallet-history-group" key={sale.id}><h2>{sale.customerDisplayName}</h2><span>{formatDateTime(sale.completedAt)}</span><ul className="wallet-tx-list">{sale.items.map((item,index)=><li className="wallet-tx-row" key={`${sale.id}-${index}`}><span>{item.productName} · {item.quantity} × {formatToman(item.unitPrice)}</span><strong>{formatToman(item.lineTotal)}</strong></li>)}</ul><strong>مجموع: {formatToman(sale.totalAmount)}</strong></article>)}</div>}</DashboardShell>}
