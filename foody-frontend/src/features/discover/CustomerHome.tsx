import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import type { Business } from "../../types/api";
import { useToast } from "../../components/Feedback";
import { WalletIcon, CalendarCheckIcon } from "../../components/icons";
import { NearbyBusinessCard } from "./NearbyBusinessCard";
import { walletApi } from "../wallet/walletApi";
import { formatToman } from "../../lib/format";

interface QuickAction {
  key: string;
  label: string;
  icon: ReactNode;
  onClick: () => void;
  accent?: "ember" | "violet" | "pistachio";
}

export function CustomerHome({ nearbyBusinesses }: { nearbyBusinesses: Business[] }) {
  const { notify } = useToast();
  const navigate = useNavigate();
  const comingSoon = (label: string) => notify(`${label} — این قابلیت به‌زودی فعال می‌شه.`);

  const {
    data: wallet,
    isLoading: isWalletLoading,
    isError: isWalletError,
    refetch: refetchWallet,
  } = useQuery({
    queryKey: ["wallet", "balance"],
    queryFn: walletApi.getBalance,
  });

  const quickActions: QuickAction[] = [
    {
      key: "qr",
      label: "اسکن QR میز",
      icon: <span className="quick-action-emoji">📷</span>,
      onClick: () => comingSoon("اسکن QR میز"),
      accent: "violet",
    },
    {
      key: "reserve",
      label: "رزرو میز",
      icon: <CalendarCheckIcon size={20} />,
      onClick: () => document.getElementById("nearby-businesses")?.scrollIntoView({ behavior: "smooth" }),
      accent: "ember",
    },
    {
      key: "topup",
      label: "شارژ کیف پول",
      icon: <WalletIcon size={20} />,
      onClick: () => navigate("/wallet"),
      accent: "pistachio",
    },
    {
      key: "offers",
      label: "پیشنهادها",
      icon: <span className="quick-action-emoji">🎁</span>,
      onClick: () => comingSoon("پیشنهادهای ویژه"),
      accent: "violet",
    },
  ];

  return (
    <div className="customer-home container">
      <section className="wallet-strip">
        <div className="wallet-card">
          <div className="wallet-card-icon">
            <WalletIcon size={26} />
          </div>
          <div className="wallet-card-body">
            <span className="wallet-card-label">موجودی کیف پول شما</span>
            {isWalletLoading ? (
              <span className="wallet-card-amount wallet-card-amount-loading">در حال بارگذاری...</span>
            ) : isWalletError ? (
              <button type="button" className="wallet-card-retry" onClick={() => refetchWallet()}>
                خطا در دریافت موجودی — تلاش دوباره
              </button>
            ) : (
              <span className="wallet-card-amount">{formatToman(wallet?.balance ?? "0")}</span>
            )}
          </div>
        </div>
      </section>

      <section className="quick-actions">
        {quickActions.map((a) => (
          <button key={a.key} type="button" className="quick-action" onClick={a.onClick}>
            <span className={`quick-action-icon ${a.accent && a.accent !== "ember" ? `quick-action-icon-${a.accent}` : ""}`}>
              {a.icon}
            </span>
            <span className="quick-action-label">{a.label}</span>
          </button>
        ))}
      </section>

      {nearbyBusinesses.length > 0 && (
        <section id="nearby-businesses" className="nearby-section">
          <div className="nearby-section-head">
            <h2>کافه‌ها را کشف کنید</h2>
          </div>
          <div className="nearby-scroll">
            {nearbyBusinesses.map((b) => (
              <NearbyBusinessCard key={b.id} business={b} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
