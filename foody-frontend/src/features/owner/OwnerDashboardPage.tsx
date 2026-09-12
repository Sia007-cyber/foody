import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { DashboardShell } from "../../components/DashboardShell";
import { BusinessStatusBadge } from "../../components/Badge";
import { ErrorState, PageSpinner, Panel } from "../../components/Controls";
import { StoreIcon, WalletIcon, CalendarCheckIcon, ReceiptIcon } from "../../components/icons";
import { resolveMediaUrl } from "../../lib/api";
import { businessApi } from "../businesses/businessApi";
import { ownerNavItems } from "./ownerNav";

export function OwnerDashboardPage(){const query=useQuery({queryKey:["business","profile"],queryFn:businessApi.myProfile});return <DashboardShell navItems={ownerNavItems} title="داشبورد" actions={query.data&&<BusinessStatusBadge status={query.data.status}/>}>{query.isLoading?<PageSpinner/>:query.isError?<ErrorState title="داشبورد لود نشد" error={query.error} onRetry={()=>query.refetch()}/>:query.data&&<><div className="branch-card">{query.data.coverImageUrl?<img src={resolveMediaUrl(query.data.coverImageUrl)??undefined} alt={query.data.name} className="branch-card-image"/>:<span className="branch-card-icon"><StoreIcon size={24}/></span>}<div className="branch-card-meta"><strong>{query.data.name}</strong><span>{query.data.address??"آدرسی ثبت نشده"}</span></div></div><div className="admin-grid"><Panel title="فروش حضوری" icon={<ReceiptIcon size={18}/>}><p>محصولات مصرف‌شده را ثبت کنید تا مشتری برداشت کیف پول را تایید کند.</p><Link className="btn btn-primary btn-sm" to="/business/new-sale">ثبت خرید حضوری</Link> <Link className="btn btn-secondary btn-sm" to="/business/sales">تاریخچه فروش</Link></Panel><Panel title="مدیریت روزانه" icon={<WalletIcon size={18}/>}><div className="owner-quick-actions"><Link to="/business/wallets">مدیریت اعتبار</Link><Link to="/business/reservations"><CalendarCheckIcon size={17}/> رزروها</Link></div></Panel></div></>}</DashboardShell>}
