import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "../../components/Button";
import { EmptyState, ErrorState, PageSpinner } from "../../components/Controls";
import { DashboardShell } from "../../components/DashboardShell";
import { Input } from "../../components/Field";
import { errorMessage, useToast } from "../../components/Feedback";
import { formatDateTime, formatToman } from "../../lib/format";
import type { Business, User, Wallet, WalletTransactionType } from "../../types/api";
import { adminApi } from "./adminApi";
import { adminNavItems } from "./adminNav";
import { adminWalletApi } from "./adminWalletApi";
import "./adminWallet.css";

const ADMIN_WALLETS_QUERY_KEY = ["admin", "wallets"] as const;
const ADMIN_WALLET_TRANSACTIONS_QUERY_KEY = ["admin", "wallets", "transactions"] as const;

const TX_TYPE_LABEL: Record<WalletTransactionType, string> = {
  OWNER_CREDIT: "افزایش اعتبار توسط کسب‌وکار",
  OWNER_DEBIT: "برداشت تأییدشده توسط کسب‌وکار",
  ADMIN_CREDIT: "افزایش اعتبار توسط مدیریت",
  ADMIN_DEBIT: "برداشت فوری توسط مدیریت",
};

function isCredit(type: WalletTransactionType) {
  return type === "OWNER_CREDIT" || type === "ADMIN_CREDIT";
}

export function AdminWalletPage() {
  const walletsQuery = useQuery({
    queryKey: ADMIN_WALLETS_QUERY_KEY,
    queryFn: adminWalletApi.getWallets,
  });
  const customersQuery = useQuery({
    queryKey: ["admin", "users", "wallet-customers"],
    queryFn: () => adminApi.users("CUSTOMER"),
  });
  const businessesQuery = useQuery({
    queryKey: ["admin", "businesses", "wallet-context"],
    queryFn: () => adminApi.businesses(),
  });
  const customers = new Map((customersQuery.data ?? []).map((customer) => [customer.id, customer]));
  const businesses = new Map((businessesQuery.data ?? []).map((business) => [business.id, business]));

  return (
    <DashboardShell navItems={adminNavItems} title="مدیریت کیف پول‌ها">
      {walletsQuery.isLoading ? (
        <PageSpinner />
      ) : walletsQuery.isError ? (
        <ErrorState error={walletsQuery.error} onRetry={() => walletsQuery.refetch()} title="کیف پول‌ها لود نشدن" />
      ) : !walletsQuery.data || walletsQuery.data.length === 0 ? (
        <EmptyState title="کیف پولی برای نمایش وجود ندارد" description="کیف پول‌ها پس از ایجاد برای مشتری و کسب‌وکار اینجا ظاهر می‌شوند." />
      ) : (
        <div className="admin-wallet-list" aria-label="کیف پول‌های کسب‌وکارها">
          {walletsQuery.data.map((wallet) => (
            <AdminWalletCard
              key={wallet.id}
              wallet={wallet}
              customer={customers.get(wallet.customerUserId)}
              business={businesses.get(wallet.businessId)}
            />
          ))}
        </div>
      )}
    </DashboardShell>
  );
}

function AdminWalletCard({ wallet, customer, business }: { wallet: Wallet; customer?: User; business?: Business }) {
  const [creditAmount, setCreditAmount] = useState("");
  const [debitAmount, setDebitAmount] = useState("");
  const [showHistory, setShowHistory] = useState(false);
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const transactionsQuery = useQuery({
    queryKey: [...ADMIN_WALLET_TRANSACTIONS_QUERY_KEY, wallet.id],
    queryFn: () => adminWalletApi.getTransactions(wallet.id),
    enabled: showHistory,
  });

  const invalidateWalletData = () => {
    queryClient.invalidateQueries({ queryKey: ADMIN_WALLETS_QUERY_KEY });
    queryClient.invalidateQueries({ queryKey: ADMIN_WALLET_TRANSACTIONS_QUERY_KEY });
  };
  const creditMutation = useMutation({
    mutationFn: (amount: string) => adminWalletApi.credit(wallet.businessId, wallet.customerUserId, amount),
    onSuccess: () => {
      invalidateWalletData();
      setCreditAmount("");
      notify("اعتبار مشتری اضافه شد", "ok");
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });
  const debitMutation = useMutation({
    mutationFn: (amount: string) => adminWalletApi.debit(wallet.businessId, wallet.customerUserId, amount),
    onSuccess: () => {
      invalidateWalletData();
      setDebitAmount("");
      notify("مبلغ فوراً از اعتبار مشتری کم شد", "ok");
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });

  const submit = (event: FormEvent<HTMLFormElement>, amount: string, mutate: (value: string) => void) => {
    event.preventDefault();
    mutate(amount);
  };
  const customerLabel = customer ? `${customer.fullName} · ${customer.email ?? customer.phone ?? "بدون راه ارتباطی"}` : `مشتری #${wallet.customerUserId}`;
  const businessLabel = business ? business.name : `کسب‌وکار #${wallet.businessId}`;

  return (
    <article className="admin-wallet-card">
      <div className="admin-wallet-summary">
        <div>
          <h2>{customerLabel}</h2>
          <p>کسب‌وکار: <strong>{businessLabel}</strong></p>
          <p>شناسه مشتری: {wallet.customerUserId} · شناسه کسب‌وکار: {wallet.businessId}</p>
        </div>
        <strong className="admin-wallet-balance">{formatToman(wallet.balance)}</strong>
      </div>

      <div className="admin-wallet-actions">
        <form onSubmit={(event) => submit(event, creditAmount, creditMutation.mutate)} className="admin-wallet-action-form">
          <Input aria-label={`مبلغ افزایش اعتبار ${customerLabel}`} label="افزایش اعتبار" inputMode="decimal" min="0.01" required step="0.01" type="number" value={creditAmount} onChange={(event) => setCreditAmount(event.target.value)} />
          <Button type="submit" size="sm" loading={creditMutation.isPending}>افزودن اعتبار</Button>
        </form>
        <form onSubmit={(event) => submit(event, debitAmount, debitMutation.mutate)} className="admin-wallet-action-form admin-wallet-debit-form">
          <Input aria-label={`مبلغ برداشت فوری ${customerLabel}`} label="برداشت فوری مدیریت" inputMode="decimal" min="0.01" required step="0.01" type="number" value={debitAmount} onChange={(event) => setDebitAmount(event.target.value)} />
          <div>
            <Button type="submit" size="sm" variant="danger" loading={debitMutation.isPending}>کسر فوری اعتبار</Button>
            <p>این عملیات فوری است و به تأیید مشتری یا درخواست برداشت نیاز ندارد.</p>
          </div>
        </form>
      </div>

      <div className="admin-wallet-history">
        <Button type="button" size="sm" variant="secondary" onClick={() => setShowHistory((visible) => !visible)}>
          {showHistory ? "بستن تاریخچه تراکنش‌ها" : "مشاهده تاریخچه تراکنش‌ها"}
        </Button>
        {showHistory && (transactionsQuery.isLoading ? <PageSpinner /> : transactionsQuery.isError ? (
          <ErrorState error={transactionsQuery.error} onRetry={() => transactionsQuery.refetch()} title="تاریخچه تراکنش‌ها لود نشد" />
        ) : !transactionsQuery.data || transactionsQuery.data.length === 0 ? <EmptyState title="تراکنشی برای این کیف پول وجود ندارد" /> : (
          <ul className="admin-wallet-transaction-list">
            {transactionsQuery.data.map((transaction) => {
              const credit = isCredit(transaction.type);
              return <li key={transaction.id}>
                <span>{TX_TYPE_LABEL[transaction.type]}</span>
                <span>{formatDateTime(transaction.createdAt)}</span>
                <strong className={credit ? "credit" : "debit"}>{credit ? "+" : "−"}{formatToman(transaction.amount)}</strong>
                <span>مانده: {formatToman(transaction.balanceAfter)}</span>
              </li>;
            })}
          </ul>
        ))}
      </div>
    </article>
  );
}
