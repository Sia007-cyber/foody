import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "../../components/Button";
import { PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { DashboardShell } from "../../components/DashboardShell";
import { Input } from "../../components/Field";
import { errorMessage, useToast } from "../../components/Feedback";
import { WalletIcon } from "../../components/icons";
import { formatToman } from "../../lib/format";
import type { Wallet } from "../../types/api";
import { ownerNavItems } from "./ownerNav";
import { ownerWalletApi } from "./ownerWalletApi";
import "./ownerWallet.css";

const OWNER_WALLETS_QUERY_KEY = ["business", "wallets"] as const;

export function OwnerWalletPage() {
  const walletsQuery = useQuery({
    queryKey: OWNER_WALLETS_QUERY_KEY,
    queryFn: ownerWalletApi.getWallets,
  });

  return (
    <DashboardShell navItems={ownerNavItems} title="مدیریت اعتبار مشتری‌ها">
      {walletsQuery.isLoading ? (
        <PageSpinner />
      ) : walletsQuery.isError ? (
        <ErrorState error={walletsQuery.error} onRetry={() => walletsQuery.refetch()} title="کیف پول مشتری‌ها لود نشد" />
      ) : !walletsQuery.data || walletsQuery.data.length === 0 ? (
        <EmptyState title="هنوز کیف پول مشتری‌ای نداری" description="وقتی برای مشتری این کسب‌وکار کیف پولی ایجاد شود، اینجا نمایش داده می‌شود." />
      ) : (
        <div className="owner-wallet-list" aria-label="کیف پول مشتری‌ها">
          {walletsQuery.data.map((wallet) => <CustomerWalletCard key={wallet.id} wallet={wallet} />)}
        </div>
      )}
    </DashboardShell>
  );
}

function CustomerWalletCard({ wallet }: { wallet: Wallet }) {
  const [creditAmount, setCreditAmount] = useState("");
  const [debitAmount, setDebitAmount] = useState("");
  const queryClient = useQueryClient();
  const { notify } = useToast();

  const creditMutation = useMutation({
    mutationFn: (amount: string) => ownerWalletApi.creditCustomer(wallet.customerUserId, amount),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: OWNER_WALLETS_QUERY_KEY });
      setCreditAmount("");
      notify("اعتبار مشتری اضافه شد", "ok");
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });
  const debitMutation = useMutation({
    mutationFn: (amount: string) => ownerWalletApi.createDebitRequest(wallet.customerUserId, amount),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: OWNER_WALLETS_QUERY_KEY });
      setDebitAmount("");
      notify("درخواست برداشت ثبت شد", "ok");
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });

  const submit = (event: FormEvent<HTMLFormElement>, amount: string, mutate: (value: string) => void) => {
    event.preventDefault();
    mutate(amount);
  };

  return (
    <article className="owner-wallet-card">
      <div className="owner-wallet-summary">
        <span className="owner-wallet-icon"><WalletIcon size={23} /></span>
        <div>
          <h2>مشتری #{wallet.customerUserId}</h2>
          <p>موجودی فعلی: <strong>{formatToman(wallet.balance)}</strong></p>
        </div>
      </div>

      <div className="owner-wallet-actions">
        <form onSubmit={(event) => submit(event, creditAmount, creditMutation.mutate)} className="owner-wallet-action-form">
          <Input
            aria-label={`مبلغ افزایش اعتبار مشتری ${wallet.customerUserId}`}
            label="افزایش اعتبار"
            inputMode="decimal"
            min="0.01"
            required
            step="0.01"
            type="number"
            value={creditAmount}
            onChange={(event) => setCreditAmount(event.target.value)}
          />
          <Button type="submit" size="sm" loading={creditMutation.isPending}>افزودن اعتبار</Button>
        </form>

        <form onSubmit={(event) => submit(event, debitAmount, debitMutation.mutate)} className="owner-wallet-action-form owner-wallet-debit-form">
          <Input
            aria-label={`مبلغ درخواست برداشت مشتری ${wallet.customerUserId}`}
            label="درخواست برداشت"
            inputMode="decimal"
            min="0.01"
            required
            step="0.01"
            type="number"
            value={debitAmount}
            onChange={(event) => setDebitAmount(event.target.value)}
          />
          <div className="owner-wallet-debit-controls">
            <Button type="submit" size="sm" variant="secondary" loading={debitMutation.isPending}>ثبت درخواست برداشت</Button>
            <p>این فقط درخواست است؛ تا تأیید مشتری، از موجودی کم نمی‌شود.</p>
          </div>
        </form>
      </div>
    </article>
  );
}
