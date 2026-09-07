import { useMemo } from "react";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "../auth/AuthContext";
import { businessApi } from "../businesses/businessApi";
import { Button } from "../../components/Button";
import { PageSpinner, ErrorState, EmptyState } from "../../components/Controls";
import { useToast, errorMessage } from "../../components/Feedback";
import { CheckCircleIcon, ClockIcon, TrendDownIcon, TrendUpIcon, WalletIcon } from "../../components/icons";
import { formatToman, formatDateTime } from "../../lib/format";
import type { DebitRequestStatus, WalletTransactionType } from "../../types/api";
import { walletApi } from "./walletApi";
import "./wallet.css";

const TX_TYPE_LABEL: Record<WalletTransactionType, string> = {
  OWNER_CREDIT: "اعتبار از کسب‌وکار",
  OWNER_DEBIT: "برداشت تاییدشده کسب‌وکار",
  ADMIN_CREDIT: "اعتبار از مدیریت",
  ADMIN_DEBIT: "برداشت مدیریت",
};

const REQUEST_STATUS_LABEL: Record<DebitRequestStatus, string> = {
  PENDING: "در انتظار پاسخ",
  APPROVED: "تاییدشده",
  REJECTED: "ردشده",
};

function isCredit(type: WalletTransactionType) {
  return type === "OWNER_CREDIT" || type === "ADMIN_CREDIT";
}

export function WalletPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const walletsQuery = useQuery({ queryKey: ["wallet", "wallets"], queryFn: walletApi.getWallets });
  const requestsQuery = useQuery({ queryKey: ["wallet", "debit-requests"], queryFn: walletApi.getPendingDebitRequests });
  const wallets = walletsQuery.data ?? [];
  const pendingRequests = requestsQuery.data ?? [];
  const businessIds = useMemo(
    () => Array.from(new Set([...wallets.map((wallet) => wallet.businessId), ...pendingRequests.map((request) => request.businessId)])),
    [wallets, pendingRequests],
  );
  const businessQueries = useQueries({
    queries: businessIds.map((businessId) => ({
      queryKey: ["businesses", businessId],
      queryFn: () => businessApi.getById(businessId),
    })),
  });
  const transactionsQueries = useQueries({
    queries: wallets.map((wallet) => ({
      queryKey: ["wallet", "transactions", wallet.id],
      queryFn: () => walletApi.getTransactions(wallet.id),
    })),
  });
  const businessById = new Map(businessIds.map((id, index) => [id, businessQueries[index]]));

  const resolveRequest = useMutation({
    mutationFn: ({ requestId, action }: { requestId: number; action: "approve" | "reject" }) =>
      action === "approve" ? walletApi.approveDebitRequest(requestId) : walletApi.rejectDebitRequest(requestId),
    onSuccess: (_request, { action }) => {
      queryClient.invalidateQueries({ queryKey: ["wallet", "wallets"] });
      queryClient.invalidateQueries({ queryKey: ["wallet", "transactions"] });
      queryClient.invalidateQueries({ queryKey: ["wallet", "debit-requests"] });
      notify(action === "approve" ? "درخواست برداشت تایید شد" : "درخواست برداشت رد شد", "ok");
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });

  const businessName = (businessId: number) => businessById.get(businessId)?.data?.name;
  const businessQuery = (businessId: number) => businessById.get(businessId);

  async function copyPublicId() {
    if (!user?.publicId) return;
    try {
      await navigator.clipboard.writeText(user.publicId);
      notify("شناسه فودی کپی شد", "ok");
    } catch {
      notify("کپی شناسه فودی انجام نشد؛ دوباره تلاش کنید.", "danger");
    }
  }

  if (walletsQuery.isLoading) return <PageSpinner />;
  if (walletsQuery.isError) return <ErrorState error={walletsQuery.error} onRetry={() => walletsQuery.refetch()} title="کیف پول‌ها لود نشدن" />;

  return (
    <div className="container wallet-page">
      <header className="wallet-hero">
        <div className="wallet-hero-orb wallet-hero-orb-violet" aria-hidden="true" />
        <div className="wallet-hero-orb wallet-hero-orb-pistachio" aria-hidden="true" />
        <div className="wallet-hero-heading">
          <span className="wallet-hero-kicker"><WalletIcon size={18} /> اعتبارهای فودی</span>
          <h1 className="wallet-page-title">کیف پول من</h1>
          <p>اعتبارتان را در هر کافه مدیریت کنید و درخواست‌های برداشت را با خیال راحت پاسخ دهید.</p>
        </div>

        {user?.publicId && (
          <section className="wallet-public-id" aria-labelledby="wallet-foody-id-title">
            <div className="wallet-public-id-copy">
              <span id="wallet-foody-id-title" className="wallet-public-id-label">شناسه فودی من</span>
              <code dir="ltr">{user.publicId}</code>
              <p>این شناسه را با کافه به اشتراک بگذارید تا برایتان اعتبار ثبت کند.</p>
            </div>
            <Button type="button" size="sm" variant="secondary" className="wallet-copy-button" onClick={copyPublicId} aria-label="کپی شناسه فودی">
              کپی شناسه
            </Button>
          </section>
        )}
      </header>

      <section className="wallet-balances" aria-labelledby="wallet-balances-title">
        <div className="wallet-section-heading">
          <div><span className="wallet-section-eyebrow">موجودی‌ها</span><h2 id="wallet-balances-title">اعتبار هر کافه</h2></div>
          <span className="wallet-section-count">{wallets.length} کیف پول</span>
        </div>
        {wallets.length === 0 ? <div className="wallet-empty-card"><WalletIcon size={28} /><EmptyState title="هنوز کیف پولی نداری" description="با دریافت اعتبار از یک کافه، کیف پول آن اینجا ایجاد و نمایش داده می‌شود." /></div> : (
          <div className="wallet-balance-grid">
            {wallets.map((wallet) => {
              const business = businessQuery(wallet.businessId);
              return <article key={wallet.id} className="wallet-balance-card">
                <div className="wallet-balance-card-top">
                  <div className="wallet-balance-icon"><WalletIcon size={26} /></div>
                  <span className="wallet-balance-caption">اعتبار قابل استفاده</span>
                </div>
                <div className="wallet-balance-body">
                  <span className="wallet-balance-label">{business?.isLoading ? "در حال دریافت نام کسب‌وکار..." : business?.isError ? "نام کسب‌وکار دریافت نشد" : businessName(wallet.businessId) ?? "کسب‌وکار فودی"}</span>
                  {business?.isError && <button type="button" className="wallet-card-retry" onClick={() => business.refetch()}>تلاش دوباره</button>}
                  <span className="wallet-balance-amount">{formatToman(wallet.balance)}</span>
                </div>
              </article>;
            })}
          </div>
        )}
      </section>

      <section className="wallet-requests" aria-labelledby="wallet-requests-title">
        <div className="wallet-section-heading"><div><span className="wallet-section-eyebrow">نیازمند پاسخ شما</span><h2 id="wallet-requests-title">درخواست‌های برداشت</h2></div></div>
        {requestsQuery.isLoading ? <PageSpinner /> : requestsQuery.isError ? (
          <ErrorState error={requestsQuery.error} onRetry={() => requestsQuery.refetch()} title="درخواست‌ها لود نشدن" />
        ) : pendingRequests.length === 0 ? <div className="wallet-empty-card wallet-empty-card-compact"><CheckCircleIcon size={25} /><EmptyState title="درخواست برداشت در انتظاری نداری" description="درخواست‌های جدید اینجا برای تأیید یا رد کردن نمایش داده می‌شوند." /></div> : (
          <ul className="wallet-request-list">
            {pendingRequests.map((request) => {
              const business = businessQuery(request.businessId);
              const resolving = resolveRequest.isPending && resolveRequest.variables?.requestId === request.id;
              return <li key={request.id} className="wallet-request-row">
                <div className="wallet-request-main">
                  <div className="wallet-request-title-row"><span className="wallet-request-business">{business?.isLoading ? "در حال دریافت نام کسب‌وکار..." : business?.isError ? "نام کسب‌وکار دریافت نشد" : businessName(request.businessId) ?? "کسب‌وکار فودی"}</span><span className="wallet-request-status"><ClockIcon size={14} />{REQUEST_STATUS_LABEL[request.status]}</span></div>
                  <strong className="wallet-request-amount">{formatToman(request.amount)}</strong>
                  <span className="wallet-tx-date">{formatDateTime(request.createdAt)}</span>
                </div>
                <div className="wallet-request-actions">
                  <Button size="sm" variant="ok" loading={resolving && resolveRequest.variables.action === "approve"} disabled={resolveRequest.isPending} onClick={() => resolveRequest.mutate({ requestId: request.id, action: "approve" })}>تایید</Button>
                  <Button size="sm" variant="danger" loading={resolving && resolveRequest.variables.action === "reject"} disabled={resolveRequest.isPending} onClick={() => resolveRequest.mutate({ requestId: request.id, action: "reject" })}>رد کردن</Button>
                </div>
              </li>;
            })}
          </ul>
        )}
      </section>

      <section className="wallet-history" aria-labelledby="wallet-history-title">
        <div className="wallet-section-heading"><div><span className="wallet-section-eyebrow">رفت‌وآمد اعتبار</span><h2 id="wallet-history-title">تاریخچه تراکنش‌ها</h2></div></div>
        {wallets.length === 0 ? <div className="wallet-empty-card wallet-empty-card-compact"><ClockIcon size={25} /><EmptyState title="هنوز تراکنشی نداری" description="با دریافت یا استفاده از اعتبار، جزئیات آن را اینجا می‌بینید." /></div> : (
          <div className="wallet-history-groups">
            {wallets.map((wallet, index) => {
              const transactions = transactionsQueries[index];
              const business = businessQuery(wallet.businessId);
              return <section key={wallet.id} className="wallet-history-group">
                <h3>{business?.data?.name ?? "تراکنش‌های کسب‌وکار"}</h3>
                {transactions.isLoading ? <PageSpinner /> : transactions.isError ? (
                  <ErrorState error={transactions.error} onRetry={() => transactions.refetch()} title="تاریخچه لود نشد" />
                ) : !transactions.data || transactions.data.length === 0 ? <EmptyState title="تراکنشی برای این کسب‌وکار نداری" /> : (
                  <ul className="wallet-tx-list">
                    {transactions.data.map((transaction) => {
                      const credit = isCredit(transaction.type);
                      return <li key={transaction.id} className="wallet-tx-row">
                        <span className={`wallet-tx-direction ${credit ? "credit" : "debit"}`} aria-hidden="true">{credit ? <TrendUpIcon size={18} /> : <TrendDownIcon size={18} />}</span>
                        <div className="wallet-tx-main"><span className="wallet-tx-type">{TX_TYPE_LABEL[transaction.type]}</span><span className="wallet-tx-date">{formatDateTime(transaction.createdAt)}</span></div>
                        <div className="wallet-tx-amounts"><span className={`wallet-tx-amount ${credit ? "credit" : "debit"}`}>{credit ? "+" : "−"}{formatToman(transaction.amount)}</span><span className="wallet-tx-balance-after">موجودی بعدش: {formatToman(transaction.balanceAfter)}</span></div>
                      </li>;
                    })}
                  </ul>
                )}
              </section>;
            })}
          </div>
        )}
      </section>
    </div>
  );
}
