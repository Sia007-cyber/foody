import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronStartIcon, MapPinIcon, CalendarCheckIcon, WalletIcon } from "../../components/icons";

const FEATURES = [
  {
    icon: <MapPinIcon size={16} />,
    title: "کشف کافه و رستوران‌ها",
  },
  {
    icon: <CalendarCheckIcon size={16} />,
    title: "رزرو آنلاین میز در چند ثانیه",
  },
  {
    icon: <WalletIcon size={16} />,
    title: "مدیریت اعتبار کیف پول‌های کسب‌وکارها",
  },
] as const;

const STICKERS = [
  { emoji: "☕️", tier: "near", pos: 1 },
  { emoji: "🍕", tier: "mid", pos: 2 },
  { emoji: "🥗", tier: "far", pos: 3 },
  { emoji: "🍰", tier: "mid", pos: 4 },
  { emoji: "🍔", tier: "far", pos: 5 },
  { emoji: "🧋", tier: "near", pos: 6 },
  { emoji: "🍩", tier: "far", pos: 7 },
  { emoji: "🍜", tier: "mid", pos: 8 },
  { emoji: "🥤", tier: "far", pos: 9 },
] as const;

export function AuthVisual({
  kicker,
  title,
  subtitle,
  showFeatures = true,
  children,
}: {
  kicker: string;
  title: string;
  subtitle?: string;
  /** نمایش لیست ویژگی‌ها زیر متن؛ برای صفحات کوتاه‌تر (مثل ورود) خاموش می‌شود. */
  showFeatures?: boolean;
  children: ReactNode;
}) {
  const navigate = useNavigate();

  return (
    <div className="auth-hero">
      <div className="auth-hero-orb auth-hero-orb-violet" aria-hidden="true" />
      <div className="auth-hero-orb auth-hero-orb-pistachio" aria-hidden="true" />
      <div className="auth-hero-orb auth-hero-orb-deep" aria-hidden="true" />
      <div className="auth-hero-stickers" aria-hidden="true">
        {STICKERS.map((s) => (
          <span key={s.pos} className={`auth-hero-sticker auth-hero-sticker-${s.tier} auth-hero-sticker-${s.pos}`}>
            {s.emoji}
          </span>
        ))}
      </div>

      <div className="auth-hero-topbar">
        <button
          type="button"
          className="auth-hero-back-btn"
          aria-label="بازگشت"
          onClick={() => navigate("/")}
        >
          <ChevronStartIcon size={18} className="auth-hero-back-icon" />
        </button>
        <span className="auth-hero-brand">
          فودی<span className="auth-hero-brand-dot">.</span>
        </span>
      </div>

      <div className="auth-hero-inner">
        <div className="auth-hero-text">
          <span className="auth-hero-kicker">{kicker}</span>
          <h1 className="auth-hero-title">{title}</h1>
          {subtitle && <p className="auth-hero-subtitle">{subtitle}</p>}
          {showFeatures && (
            <ul className="auth-hero-features">
              {FEATURES.map((f) => (
                <li key={f.title} className="auth-hero-feature">
                  <span className="auth-hero-feature-icon">{f.icon}</span>
                  {f.title}
                </li>
              ))}
            </ul>
          )}
        </div>

        {children}
      </div>
    </div>
  );
}
