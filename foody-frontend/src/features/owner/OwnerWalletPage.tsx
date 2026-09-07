import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "../../components/Button";
import { PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import { DashboardShell } from "../../components/DashboardShell";
import { Input } from "../../components/Field";
import { errorMessage, useToast } from "../../components/Feedback";
import { WalletIcon } from "../../components/icons";
import { formatToman } from "../../lib/format";
import type { CustomerLookup, OwnerWallet } from "../../types/api";
import { ownerNavItems } from "./ownerNav";
import { ownerWalletApi } from "./ownerWalletApi";
import "./ownerWallet.css";

const OWNER_WALLETS_QUERY_KEY = ["business", "wallets"] as const;

export function OwnerWalletPage() {
  const walletsQuery = useQuery({
    queryKey: OWNER_WALLETS_QUERY_KEY,
    queryFn: ownerWalletApi.getWallets,
  });
  const [publicId, setPublicId] = useState("");
  const [foundCustomer, setFoundCustomer] = useState<CustomerLookup | null>(null);
  const [firstCreditAmount, setFirstCreditAmount] = useState("");
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const lookupMutation = useMutation({
    mutationFn: () => ownerWalletApi.lookupCustomer(publicId.trim().toUpperCase()),
    onSuccess: setFoundCustomer,
    onError: (error) => { setFoundCustomer(null); notify(errorMessage(error), "danger"); },
  });
  const firstCreditMutation = useMutation({
    mutationFn: () => ownerWalletApi.creditCustomer(foundCustomer!.publicId, firstCreditAmount),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: OWNER_WALLETS_QUERY_KEY });
      setFirstCreditAmount("");
      setFoundCustomer(null);
      setPublicId("");
      notify("اعتبار مشتری اضافه شد", "ok");
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });

  return (
    <DashboardShell navItems={ownerNavItems} title="مدیریت اعتبار مشتری‌ها">
      <section className="owner-customer-lookup" aria-labelledby="customer-lookup-title">
        <h2 id="customer-lookup-title">افزودن اعتبار با شناسه فودی</h2>
        <form onSubmit={(event) => { event.preventDefault(); lookupMutation.mutate(); }} className="owner-wallet-action-form">
          <Input label="شناسه فودی مشتری" dir="ltr" placeholder="F-…" required value={publicId}
            onChange={(event) => { setPublicId(event.target.value); setFoundCustomer(null); }} />
          <Button type="submit" size="sm" loading={lookupMutation.isPending}>جست‌وجوی مشتری</Button>
        </form>
        {foundCustomer && (
          <div className="owner-customer-confirmation">
            <p><strong>{foundCustomer.displayName}</strong></p>
            <code dir="ltr">{foundCustomer.publicId}</code>
            <form onSubmit={(event) => { event.preventDefault(); firstCreditMutation.mutate(); }} className="owner-wallet-action-form">
              <Input label="مبلغ اعتبار" inputMode="decimal" min="0.01" required step="0.01" type="number"
                value={firstCreditAmount} onChange={(event) => setFirstCreditAmount(event.target.value)} />
              <Button type="submit" size="sm" loading={firstCreditMutation.isPending}>افزودن اعتبار</Button>
            </form>
          </div>
        )}
      </section>
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

function CustomerWalletCard({ wallet }: { wallet: OwnerWallet }) {
  const [creditAmount, setCreditAmount] = useState("");
  const [debitAmount, setDebitAmount] = useState("");
  const queryClient = useQueryClient();
  const { notify } = useToast();

  const creditMutation = useMutation({
    mutationFn: (amount: string) => ownerWalletApi.creditCustomer(wallet.customerPublicId, amount),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: OWNER_WALLETS_QUERY_KEY });
      setCreditAmount("");
      notify("اعتبار مشتری اضافه شد", "ok");
    },
    onError: (error) => notify(errorMessage(error), "danger"),
  });
  const debitMutation = useMutation({
    mutationFn: (amount: string) => ownerWalletApi.createDebitRequest(wallet.customerPublicId, amount),
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
          <h2>{wallet.customerDisplayName}</h2>
          <code dir="ltr">{wallet.customerPublicId}</code>
          <p>موجودی فعلی: <strong>{formatToman(wallet.balance)}</strong></p>
        </div>
      </div>

      <div className="owner-wallet-actions">
        <form onSubmit={(event) => submit(event, creditAmount, creditMutation.mutate)} className="owner-wallet-action-form">
          <Input
            aria-label={`مبلغ افزایش اعتبار مشتری ${wallet.customerPublicId}`}
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
            aria-label={`مبلغ درخواست برداشت مشتری ${wallet.customerPublicId}`}
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
