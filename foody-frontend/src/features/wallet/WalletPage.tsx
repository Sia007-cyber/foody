import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { walletApi } from "./walletApi";
import { Input } from "../../components/Field";
import { Button } from "../../components/Button";
import { PageSpinner, ErrorState, EmptyState } from "../../components/Controls";
import { useToast, errorMessage } from "../../components/Feedback";
import { WalletIcon } from "../../components/icons";
import { formatToman, formatDateTime } from "../../lib/format";
import type { WalletTransactionType } from "../../types/api";
import "./wallet.css";

const TX_TYPE_LABEL: Record<WalletTransactionType, string> = {
  TOPUP: "شارژ کیف پول",
  ORDER_PAYMENT: "پرداخت سفارش",
  REFUND: "بازگشت وجه",
  CREDIT_REWARD: "پاداش اعتبار",
  ADMIN_ADJUSTMENT: "تنظیم توسط ادمین",
};

const QUICK_AMOUNTS = [50000, 100000, 200000, 500000];

export function WalletPage() {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [amount, setAmount] = useState("");

  const {
    data: wallet,
    isLoading: isWalletLoading,
    isError: isWalletError,
    refetch: refetchWallet,
  } = useQuery({
    queryKey: ["wallet", "balance"],
    queryFn: walletApi.getBalance,
  });

  const {
    data: transactions,
    isLoading: isTxLoading,
    isError: isTxError,
    error: txError,
    refetch: refetchTx,
  } = useQuery({
    queryKey: ["wallet", "transactions"],
    queryFn: walletApi.getTransactions,
  });

  const topUp = useMutation({
    mutationFn: (value: number) => walletApi.topUp(value),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallet"] });
      setAmount("");
      notify("کیف پول شارژ شد", "ok");
    },
    onError: (err) => notify(errorMessage(err), "danger"),
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const value = Number(amount);
    if (!value || value < 1000) {
      notify("حداقل مبلغ شارژ ۱٬۰۰۰ تومانه", "danger");
      return;
    }
    topUp.mutate(value);
  }

  return (
    <div className="container wallet-page">
      <h1 className="wallet-page-title">کیف پول من</h1>

      <div className="wallet-balance-card">
        <div className="wallet-balance-icon">
          <WalletIcon size={26} />
        </div>
        <div className="wallet-balance-body">
          <span className="wallet-balance-label">موجودی فعلی</span>
          {isWalletLoading ? (
            <span className="wallet-balance-amount wallet-balance-loading">در حال بارگذاری...</span>
          ) : isWalletError ? (
            <button type="button" className="wallet-card-retry" onClick={() => refetchWallet()}>
              خطا در دریافت موجودی — تلاش دوباره
            </button>
          ) : (
            <span className="wallet-balance-amount">{formatToman(wallet?.balance ?? "0")}</span>
          )}
        </div>
      </div>

      <form onSubmit={handleSubmit} className="wallet-topup-form">
        <span className="profile-section-label">شارژ کیف پول</span>
        <div className="wallet-quick-amounts">
          {QUICK_AMOUNTS.map((a) => (
            <button
              key={a}
              type="button"
              className={`wallet-quick-amount ${Number(amount) === a ? "active" : ""}`}
              onClick={() => setAmount(String(a))}
            >
              {formatToman(a)}
            </button>
          ))}
        </div>
        <div className="wallet-topup-row">
          <Input
            type="number"
            min={1000}
            step={1000}
            placeholder="مبلغ دلخواه (تومان)"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          <Button type="submit" loading={topUp.isPending}>
            شارژ کن
          </Button>
        </div>
        <p className="wallet-topup-note">
          ⚠️ این شارژ داخلی/دستیه، پرداخت آنلاین واقعی هنوز اضافه نشده.
        </p>
      </form>

      <section className="wallet-history">
        <h2>تاریخچه‌ی تراکنش‌ها</h2>
        {isTxLoading ? (
          <PageSpinner />
        ) : isTxError ? (
          <ErrorState error={txError} onRetry={() => refetchTx()} title="تاریخچه لود نشد" />
        ) : !transactions || transactions.length === 0 ? (
          <EmptyState title="هنوز تراکنشی نداری" />
        ) : (
          <ul className="wallet-tx-list">
            {transactions.map((tx) => (
              <li key={tx.id} className="wallet-tx-row">
                <div className="wallet-tx-main">
                  <span className="wallet-tx-type">{TX_TYPE_LABEL[tx.type]}</span>
                  <span className="wallet-tx-date">{formatDateTime(tx.createdAt)}</span>
                  {tx.description && <span className="wallet-tx-desc">{tx.description}</span>}
                </div>
                <div className="wallet-tx-amounts">
                  <span className={`wallet-tx-amount ${tx.credit ? "credit" : "debit"}`}>
                    {tx.credit ? "+" : "−"}
                    {formatToman(tx.amount)}
                  </span>
                  <span className="wallet-tx-balance-after">موجودی بعدش: {formatToman(tx.balanceAfter)}</span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
