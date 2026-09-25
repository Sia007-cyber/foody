import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import type { Business, DiscoveryProduct } from "../../types/api";
import { useAuth } from "../auth/AuthContext";
import { ChevronStartIcon, WalletIcon, CalendarCheckIcon } from "../../components/icons";
import { formatToman } from "../../lib/format";
import { walletApi } from "../wallet/walletApi";
import { Segmented } from "../../components/Controls";
import { NearbyBusinessCard } from "./NearbyBusinessCard";
import { ProductCard } from "./ProductCard";

export type TypeFilter = "" | "CAFE" | "FAST_FOOD";

interface QuickAction {
  key: string;
  label: string;
  icon: ReactNode;
  onClick: () => void;
  accent: "ember" | "violet" | "pistachio";
}

// Decorative — purely visual, never carries text (pointer-events: none in CSS).
// Confined entirely to the card's left side (inset-inline-end in this RTL app),
// since the greeting/search column hugs the right (inset-inline-start) side —
// keeping the two from ever sharing horizontal space is what keeps text clear
// at every breakpoint, instead of relying on per-item nudges.
// tier: near = bigger/sharper (revealed first) / mid / far = smaller, blurred (revealed last, widest screens only)
const HOME_HERO_STICKERS: { emoji: string; key: string; tier: "near" | "mid" | "far" }[] = [
  { emoji: "🍔", key: "burger", tier: "near" },
  { emoji: "☕", key: "coffee", tier: "near" },
  { emoji: "🍕", key: "pizza", tier: "mid" },
  { emoji: "🍩", key: "donut", tier: "mid" },
  { emoji: "🍟", key: "fries", tier: "far" },
  { emoji: "🧋", key: "boba", tier: "far" },
];

interface CustomerHomeProps {
  featuredBusinesses: Business[];
  topRatedBusinesses: Business[];
  topProducts: DiscoveryProduct[];
  search: string;
  onSearchChange: (value: string) => void;
  showHero?: boolean;
  showDiscoveryContent?: boolean;
  typeFilter?: TypeFilter;
  onTypeFilterChange?: (value: TypeFilter) => void;
}

export function CustomerHome({ featuredBusinesses, topRatedBusinesses, topProducts, search, onSearchChange, showHero = true, showDiscoveryContent = true, typeFilter, onTypeFilterChange }: CustomerHomeProps) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const hasCustomerWallet = user?.role === "CUSTOMER" || user?.role === "BUSINESS_OWNER";
  const firstName = user?.fullName?.trim().split(/\s+/)[0];

  const walletsQuery = useQuery({
    queryKey: ["wallet", "wallets"],
    queryFn: walletApi.getWallets,
    enabled: hasCustomerWallet,
  });
  const wallets = walletsQuery.data ?? [];
  const totalBalance = wallets.reduce((sum, w) => sum + Number(w.balance), 0);

  const quickActions: QuickAction[] = [
    {
      key: "reserve",
      label: "رزرو میز",
      icon: <CalendarCheckIcon size={20} />,
      onClick: () => document.getElementById("all-businesses")?.scrollIntoView({ behavior: "smooth" }),
      accent: "ember",
    },
    ...(hasCustomerWallet ? [{
      key: "topup",
      label: "کیف پول‌های شما",
      icon: <WalletIcon size={20} />,
      onClick: () => navigate("/wallet"),
      accent: "pistachio" as const,
    }] : []),
    {
      key: "offers",
      label: "پیشنهادهای ویژه",
      icon: <span className="quick-action-emoji">🎁</span>,
      onClick: () => navigate("/offers"),
      accent: "violet",
    },
  ];

  // The type filter used to only reach the "all businesses" grid at the very
  // bottom (a separate, server-filtered query). Now that it sits above the
  // curated rails too, it needs to actually govern them, not just the grid.
  // Both business arrays carry `businessType`, so this is a plain client-side
  // filter. Top products don't carry their business's type in the API
  // response, so there's no reliable way to filter that rail here — it's
  // hidden while a type filter is active rather than showing results that
  // silently ignore the filter.
  const filteredTopRated = typeFilter ? topRatedBusinesses.filter((b) => b.businessType === typeFilter) : topRatedBusinesses;
  const filteredFeatured = typeFilter ? featuredBusinesses.filter((b) => b.businessType === typeFilter) : featuredBusinesses;
  const showTopProducts = !typeFilter && topProducts.length > 0;

  return (
    <div className="customer-home container">
      {showHero && <section className="home-hero">
        <div className="home-hero-orb home-hero-orb-violet" aria-hidden="true" />
        <div className="home-hero-orb home-hero-orb-pistachio" aria-hidden="true" />
        <div className="home-hero-stickers" aria-hidden="true">
          {HOME_HERO_STICKERS.map((s, i) => (
            <span key={s.key} className={`home-hero-sticker home-hero-sticker-${s.tier} home-hero-sticker-${i + 1}`}>
              {s.emoji}
            </span>
          ))}
        </div>
        <div className="home-hero-content">
          <span className="home-hero-kicker">{firstName ? `سلام ${firstName} 👋` : "سلام 👋"}</span>
          <h1 className="home-hero-title">امروز هوس چی کردی؟</h1>
          <p className="home-hero-subtitle">کافه و فست‌فودها رو پیدا کن، منوها رو ببین یا میز رزرو کن.</p>
          <div className="home-hero-search">
            <input
              className="input"
              type="search"
              placeholder="جستجوی کسب‌وکار یا محصول..."
              value={search}
              onChange={(e) => onSearchChange(e.target.value)}
            />
          </div>
        </div>
      </section>}

      {/* Wallet credit, table reservation and the business-type filter sit
          directly under the search panel — the actions Ali actually opens
          this page for, kept above the curated discovery rails below. */}
      {showDiscoveryContent && hasCustomerWallet && <button type="button" className="wallet-preview" onClick={() => navigate("/wallet")}>
        <span className="wallet-preview-left">
          <span className="wallet-preview-icon">
            <WalletIcon size={22} />
          </span>
          <span className="wallet-preview-text">
            <span className="section-eyebrow">کیف پول‌های شما</span>
            <span className="wallet-preview-hint">
              {walletsQuery.isLoading
                ? "در حال دریافت موجودی..."
                : wallets.length === 0
                  ? "هنوز اعتباری از کافه‌ها نداری"
                  : `اعتبار در ${wallets.length} کافه`}
            </span>
          </span>
        </span>
        <span className="wallet-preview-right">
          {!walletsQuery.isLoading && wallets.length > 0 && (
            <span className="wallet-preview-amount">{formatToman(totalBalance)}</span>
          )}
          <ChevronStartIcon size={18} className="wallet-preview-chevron" />
        </span>
      </button>}

      {showDiscoveryContent && <section className="quick-actions">
        {quickActions.map((a) => (
          <button key={a.key} type="button" className={`quick-action quick-action-accent-${a.accent}`} onClick={a.onClick}>
            <span className={`quick-action-icon ${a.accent !== "ember" ? `quick-action-icon-${a.accent}` : ""}`}>{a.icon}</span>
            <span className="quick-action-label">{a.label}</span>
          </button>
        ))}
      </section>}

      {showDiscoveryContent && onTypeFilterChange && (
        <div className="home-type-filter">
          <Segmented
            value={typeFilter ?? ""}
            onChange={onTypeFilterChange}
            options={[
              { value: "", label: "همه" },
              { value: "CAFE", label: "کافه" },
              { value: "FAST_FOOD", label: "فست‌فود" },
            ]}
          />
        </div>
      )}

      {showDiscoveryContent && filteredTopRated.length > 0 && (
        <section className="nearby-section" aria-labelledby="top-rated-businesses-title">
          <div className="nearby-section-head"><div><span className="section-eyebrow">بر اساس نظر مشتری‌ها</span><h2 id="top-rated-businesses-title">کسب‌وکارهای برتر</h2></div></div>
          <div className="nearby-scroll">{filteredTopRated.map((b) => <NearbyBusinessCard key={b.id} business={b} />)}</div>
        </section>
      )}

      {showDiscoveryContent && filteredFeatured.length > 0 && (
        <section className="nearby-section" aria-labelledby="featured-businesses-title">
          <div className="nearby-section-head"><div><span className="section-eyebrow">انتخاب فودی</span><h2 id="featured-businesses-title">کسب‌وکارهای پیشنهادی</h2></div></div>
          <div className="nearby-scroll">{filteredFeatured.map((b) => <NearbyBusinessCard key={b.id} business={b} />)}</div>
        </section>
      )}

      {showDiscoveryContent && showTopProducts && (
        <section className="nearby-section" aria-labelledby="top-products-title">
          <div className="nearby-section-head"><div><span className="section-eyebrow">محبوب نزد مشتری‌ها</span><h2 id="top-products-title">محصولات برتر</h2></div></div>
          <div className="product-discovery-scroll">{topProducts.map((product) => <ProductCard key={product.id} product={product} />)}</div>
        </section>
      )}
    </div>
  );
}
