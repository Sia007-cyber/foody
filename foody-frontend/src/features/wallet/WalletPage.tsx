import { useMemo } from "react";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { businessApi } from "../businesses/businessApi";
import { Button } from "../../components/Button";
import { PageSpinner, ErrorState, EmptyState } from "../../components/Controls";
import { useToast, errorMessage } from "../../components/Feedback";
import { WalletIcon } from "../../components/icons";
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

  if (walletsQuery.isLoading) return <PageSpinner />;
  if (walletsQuery.isError) return <ErrorState error={walletsQuery.error} onRetry={() => walletsQuery.refetch()} title="کیف پول‌ها لود نشدن" />;

  return (
    <div className="container wallet-page">
      <h1 className="wallet-page-title">کیف پول‌های من</h1>

      <section className="wallet-balances" aria-labelledby="wallet-balances-title">
        <h2 id="wallet-balances-title">موجودی به تفکیک کسب‌وکار</h2>
        {wallets.length === 0 ? <EmptyState title="هنوز کیف پولی نداری" description="با دریافت اعتبار از یک کسب‌وکار، کیف پول آن اینجا نمایش داده می‌شود." /> : (
          <div className="wallet-balance-grid">
            {wallets.map((wallet) => {
              const business = businessQuery(wallet.businessId);
              return <article key={wallet.id} className="wallet-balance-card">
                <div className="wallet-balance-icon"><WalletIcon size={26} /></div>
                <div className="wallet-balance-body">
                  <span className="wallet-balance-label">{business?.isLoading ? "در حال دریافت نام کسب‌وکار..." : business?.isError ? "نام کسب‌وکار دریافت نشد" : businessName(wallet.businessId)}</span>
                  {business?.isError && <button type="button" className="wallet-card-retry" onClick={() => business.refetch()}>تلاش دوباره</button>}
                  <span className="wallet-balance-amount">{formatToman(wallet.balance)}</span>
                </div>
              </article>;
            })}
          </div>
        )}
      </section>

      <section className="wallet-requests" aria-labelledby="wallet-requests-title">
        <h2 id="wallet-requests-title">درخواست‌های برداشت</h2>
        {requestsQuery.isLoading ? <PageSpinner /> : requestsQuery.isError ? (
          <ErrorState error={requestsQuery.error} onRetry={() => requestsQuery.refetch()} title="درخواست‌ها لود نشدن" />
        ) : pendingRequests.length === 0 ? <EmptyState title="درخواست برداشت در انتظاری نداری" /> : (
          <ul className="wallet-request-list">
            {pendingRequests.map((request) => {
              const business = businessQuery(request.businessId);
              const resolving = resolveRequest.isPending && resolveRequest.variables?.requestId === request.id;
              return <li key={request.id} className="wallet-request-row">
                <div className="wallet-request-main">
                  <span className="wallet-request-business">{business?.isLoading ? "در حال دریافت نام کسب‌وکار..." : business?.isError ? "نام کسب‌وکار دریافت نشد" : businessName(request.businessId)}</span>
                  <span>{formatToman(request.amount)}</span>
                  <span className="wallet-request-status">{REQUEST_STATUS_LABEL[request.status]}</span>
                  <span className="wallet-tx-date">{formatDateTime(request.createdAt)}</span>
                </div>
                <div className="wallet-request-actions">
                  <Button size="sm" loading={resolving && resolveRequest.variables.action === "approve"} disabled={resolveRequest.isPending} onClick={() => resolveRequest.mutate({ requestId: request.id, action: "approve" })}>تایید</Button>
                  <Button size="sm" variant="danger" loading={resolving && resolveRequest.variables.action === "reject"} disabled={resolveRequest.isPending} onClick={() => resolveRequest.mutate({ requestId: request.id, action: "reject" })}>رد کردن</Button>
                </div>
              </li>;
            })}
          </ul>
        )}
      </section>

      <section className="wallet-history" aria-labelledby="wallet-history-title">
        <h2 id="wallet-history-title">تاریخچه‌ی تراکنش‌ها</h2>
        {wallets.length === 0 ? <EmptyState title="هنوز تراکنشی نداری" /> : (
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
