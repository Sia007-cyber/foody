import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import type { Business } from "../../types/api";
import { useToast } from "../../components/Feedback";
import { WalletIcon, CalendarCheckIcon, MegaphoneIcon, StarIcon } from "../../components/icons";
import { NearbyBusinessCard } from "./NearbyBusinessCard";

// ---------------------------------------------------------------------------
// همه‌ی داده‌های این فایل (موجودی کیف پول، سقف اعتبار، درصد تخفیف اپ،
// ماموریت‌ها) فعلاً MOCK/ثابت هستن — چون بک‌اند فاز ۲ (wallet/rewards/qr)
// هنوز ساخته نشده. هدف این نسخه فقط هماهنگی UI با موکاپ کارفراست برای دمو.
// وقتی endpoint های واقعی آماده شدن، جای این مقادیر ثابت با useQuery به
// /api/wallet و /api/rewards/missions عوض می‌شه (به‌جز ساختار و استایل).
// ---------------------------------------------------------------------------

interface QuickAction {
  key: string;
  label: string;
  icon: ReactNode;
  onClick: () => void;
  accent?: "ember" | "violet" | "pistachio";
}

interface Mission {
  key: string;
  title: string;
  reward: string;
  icon: ReactNode;
  accent?: "ember" | "violet" | "pistachio";
}

const MOCK_WALLET_BALANCE = 500000;
const MOCK_CREDIT_LIMIT = 625000;
const MOCK_APP_DISCOUNT_PERCENT = 20;

const MOCK_MISSIONS: Mission[] = [
  { key: "invite", title: "یک دوست دعوت کن", reward: "+۴۰,۰۰۰ اعتبار", icon: <MegaphoneIcon size={20} />, accent: "violet" },
  { key: "qr", title: "یک QR اسکن کن", reward: "+۱۰,۰۰۰ اعتبار", icon: <WalletIcon size={20} />, accent: "pistachio" },
  { key: "first-order", title: "اولین خریدت رو انجام بده", reward: "+۵۰,۰۰۰ اعتبار", icon: <StarIcon size={20} />, accent: "ember" },
];

function formatToman(amount: number): string {
  return `${amount.toLocaleString("en-US")} تومان`;
}

export function CustomerHome({ nearbyBusinesses }: { nearbyBusinesses: Business[] }) {
  const { notify } = useToast();
  const comingSoon = (label: string) => notify(`${label} — این قابلیت به‌زودی فعال می‌شه.`);

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
      onClick: () => comingSoon("شارژ کیف پول"),
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
            <span className="wallet-card-amount">{formatToman(MOCK_WALLET_BALANCE)}</span>
            <span className="wallet-card-hint">
              با این موجودی تا {formatToman(MOCK_CREDIT_LIMIT)} می‌تونید خرید کنید
            </span>
          </div>
        </div>
        <div className="discount-card">
          <span className="discount-card-percent">٪{MOCK_APP_DISCOUNT_PERCENT}</span>
          <span className="discount-card-label">تخفیف خرید از اپلیکیشن</span>
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

      <section className="promo-banner">
        <div className="promo-banner-text">
          <span className="promo-banner-title">🎁 قهوه رایگان برای شما</span>
          <span className="promo-banner-desc">با اولین خرید از طریق اپلیکیشن</span>
        </div>
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => comingSoon("مشاهده‌ی کافه‌ها")}>
          مشاهده کافه‌ها
        </button>
      </section>

      {nearbyBusinesses.length > 0 && (
        <section id="nearby-businesses" className="nearby-section">
          <div className="nearby-section-head">
            <h2>کافه‌های محبوب نزدیک شما</h2>
            <Link to="#" className="nearby-see-all" onClick={(e) => e.preventDefault()}>
              مشاهده همه
            </Link>
          </div>
          <div className="nearby-scroll">
            {nearbyBusinesses.map((b) => (
              <NearbyBusinessCard key={b.id} business={b} />
            ))}
          </div>
        </section>
      )}

      <section className="missions-section">
        <h2>ماموریت‌های امروز</h2>
        <div className="missions-grid">
          {MOCK_MISSIONS.map((m) => (
            <button key={m.key} type="button" className="mission-card" onClick={() => comingSoon(m.title)}>
              <span className={`mission-icon ${m.accent && m.accent !== "ember" ? `mission-icon-${m.accent}` : ""}`}>
                {m.icon}
              </span>
              <span className="mission-title">{m.title}</span>
              <span className="mission-reward">{m.reward}</span>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}
