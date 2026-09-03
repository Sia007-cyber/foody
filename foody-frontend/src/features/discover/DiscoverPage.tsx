import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { businessApi } from "../businesses/businessApi";
import { BusinessCard } from "./BusinessCard";
import { Segmented } from "../../components/Controls";
import { EmptyState, ErrorState, PageSpinner } from "../../components/Controls";
import { Button } from "../../components/Button";
import { useAuth } from "../auth/AuthContext";
import { CustomerHome } from "./CustomerHome";
import "./discover.css";

type TypeFilter = "" | "CAFE" | "FAST_FOOD";

// استیکرهای تزئینی هیرو — فقط بصری، هیچ متنی رو نمی‌گیرن (pointer-events: none)
// tier: near = بزرگ و واضح (جلو) / mid = متوسط / far = کوچیک، محو و بلور (پشت، برای عمق بصری)
const HERO_STICKERS: { emoji: string; label: string; tier: "near" | "mid" | "far" }[] = [
  // near — بزرگ، شارپ، جلو
  { emoji: "🍔", label: "hero-sticker-1", tier: "near" },
  { emoji: "☕", label: "hero-sticker-2", tier: "near" },
  { emoji: "🍕", label: "hero-sticker-3", tier: "near" },
  { emoji: "🍩", label: "hero-sticker-4", tier: "near" },
  // mid — متوسط
  { emoji: "🍟", label: "hero-sticker-5", tier: "mid" },
  { emoji: "🥤", label: "hero-sticker-6", tier: "mid" },
  { emoji: "🍪", label: "hero-sticker-7", tier: "mid" },
  { emoji: "🧋", label: "hero-sticker-8", tier: "mid" },
  // far — کوچیک، محو و بلور، انگار پشت بقیه‌ان
  { emoji: "🍰", label: "hero-sticker-9", tier: "far" },
  { emoji: "🧁", label: "hero-sticker-10", tier: "far" },
  { emoji: "🍫", label: "hero-sticker-11", tier: "far" },
  { emoji: "🍦", label: "hero-sticker-12", tier: "far" },
  { emoji: "🥐", label: "hero-sticker-13", tier: "far" },
  { emoji: "🍬", label: "hero-sticker-14", tier: "far" },
];

export function DiscoverPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [type, setType] = useState<TypeFilter>("");

  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(search.trim()), 350);
    return () => clearTimeout(t);
  }, [search]);

  const {
    data: businesses,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["businesses", "discover", type, debouncedSearch],
    queryFn: () => businessApi.discover({ type: type || undefined, search: debouncedSearch || undefined }),
  });

  const isCustomerHome = user?.role === "CUSTOMER" || user?.role === "BUSINESS_OWNER";

  const searchAndFilter = (
    <>
      <input
        className="input"
        type="search"
        placeholder="جستجوی نام کسب‌وکار..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      <Segmented
        value={type}
        onChange={setType}
        options={[
          { value: "", label: "همه" },
          { value: "CAFE", label: "کافه" },
          { value: "FAST_FOOD", label: "فست‌فود" },
        ]}
      />
    </>
  );

  const businessResults = isLoading ? (
    <PageSpinner />
  ) : isError ? (
    <ErrorState error={error} onRetry={() => refetch()} title="کسب‌وکارها لود نشدن" />
  ) : businesses && businesses.length > 0 ? (
    <div className="business-grid">
      {businesses.map((b) => (
        <BusinessCard key={b.id} business={b} />
      ))}
    </div>
  ) : (
    <EmptyState title="چیزی پیدا نشد" description="فیلترها رو عوض کن یا اسم دیگه‌ای رو امتحان کن." />
  );

  if (isCustomerHome) {
    return (
      <div>
        <CustomerHome nearbyBusinesses={(businesses ?? []).slice(0, 8)} />

        <section className="container discover-section discover-section-customer">
          <h2 className="discover-section-title">جستجو و فیلتر کسب‌وکارها</h2>
          <div className="discover-search-row">{searchAndFilter}</div>
          {businessResults}
        </section>
      </div>
    );
  }

  return (
    <div>
      <section className="hero">
        <div className="hero-blobs" />
        <div className="hero-stickers" aria-hidden="true">
          {HERO_STICKERS.map((s) => (
            <span key={s.label} className={`hero-sticker hero-sticker-${s.tier} ${s.label}`}>
              {s.emoji}
            </span>
          ))}
        </div>
        <h1>هرچی هوس کردی، همین‌جاست</h1>
        <p>کافه و فست‌فودای اطرافت رو پیدا کن، سفارش بده یا میز رزرو کن.</p>
        <div className="hero-search">
          <input
            className="input"
            type="search"
            placeholder="جستجوی نام کسب‌وکار..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <Segmented
          value={type}
          onChange={setType}
          options={[
            { value: "", label: "همه" },
            { value: "CAFE", label: "کافه" },
            { value: "FAST_FOOD", label: "فست‌فود" },
          ]}
        />
        {!user && (
          <div className="hero-cta">
            <Button size="md" onClick={() => navigate("/register")}>
              ثبت‌نام رایگان
            </Button>
            <Button variant="secondary" size="md" onClick={() => navigate("/login")}>
              ورود
            </Button>
          </div>
        )}
      </section>

      <section className="container discover-section">{businessResults}</section>
    </div>
  );
}
