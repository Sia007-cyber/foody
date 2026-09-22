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

// Restored anonymous-home visual treatment. These are decorative only and use
// the original classes/positions so the prior Foody hero remains intact.
const HERO_STICKERS: { emoji: string; label: string; tier: "near" | "mid" | "far" }[] = [
  { emoji: "🍔", label: "hero-sticker-1", tier: "near" },
  { emoji: "☕", label: "hero-sticker-2", tier: "near" },
  { emoji: "🍕", label: "hero-sticker-3", tier: "near" },
  { emoji: "🍩", label: "hero-sticker-4", tier: "near" },
  { emoji: "🍟", label: "hero-sticker-5", tier: "mid" },
  { emoji: "🥤", label: "hero-sticker-6", tier: "mid" },
  { emoji: "🍪", label: "hero-sticker-7", tier: "mid" },
  { emoji: "🧋", label: "hero-sticker-8", tier: "mid" },
  { emoji: "🍰", label: "hero-sticker-9", tier: "far" },
  { emoji: "🧁", label: "hero-sticker-10", tier: "far" },
  { emoji: "🍫", label: "hero-sticker-11", tier: "far" },
  { emoji: "🍦", label: "hero-sticker-12", tier: "far" },
  { emoji: "🥐", label: "hero-sticker-13", tier: "far" },
  { emoji: "🍬", label: "hero-sticker-14", tier: "far" },
];

function AnonymousDiscoverHero({ search, onSearchChange }: { search: string; onSearchChange: (value: string) => void }) {
  const navigate = useNavigate();

  return (
    <section className="hero">
      <div className="hero-blobs" />
      <div className="hero-stickers" aria-hidden="true">
        {HERO_STICKERS.map((sticker) => (
          <span key={sticker.label} className={`hero-sticker hero-sticker-${sticker.tier} ${sticker.label}`}>
            {sticker.emoji}
          </span>
        ))}
      </div>
      <h1>هرچی هوس کردی، همین‌جاست</h1>
      <p>کافه و فست‌فودها رو پیدا کن، سفارش بده یا میز رزرو کن.</p>
      <div className="hero-search">
        <input
          className="input"
          type="search"
          placeholder="جستجوی نام کسب‌وکار..."
          value={search}
          onChange={(event) => onSearchChange(event.target.value)}
        />
      </div>
      <div className="hero-cta">
        <Button size="md" onClick={() => navigate("/register")}>ثبت‌نام رایگان</Button>
        <Button variant="secondary" size="md" onClick={() => navigate("/login")}>ورود</Button>
      </div>
    </section>
  );
}

export function DiscoverPage() {
  const { user } = useAuth();
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

  const featuredQuery = useQuery({ queryKey: ["businesses", "featured"], queryFn: businessApi.featured });
  const popularQuery = useQuery({ queryKey: ["businesses", "popular"], queryFn: businessApi.popular });

  // Signed-in users share one marketplace discovery experience. Actions that are
  // not available to a role are handled inside the shared home component.
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

  return (
    <div>
      {!user && <AnonymousDiscoverHero search={search} onSearchChange={setSearch} />}
      <CustomerHome
        featuredBusinesses={featuredQuery.data ?? []}
        popularBusinesses={popularQuery.data ?? []}
        search={search}
        onSearchChange={setSearch}
        showHero={user != null}
      />

      <section id="all-businesses" className="container discover-section discover-section-customer">
          <div className="discover-section-head">
            <span className="section-eyebrow">جستجو</span>
            <h2 className="discover-section-title">
              {debouncedSearch ? `نتیجه‌ی جستجو برای «${debouncedSearch}»` : "همه‌ی کسب‌وکارها"}
            </h2>
          </div>
          <div className="discover-search-row">
            <Segmented
              value={type}
              onChange={setType}
              options={[
                { value: "", label: "همه" },
                { value: "CAFE", label: "کافه" },
                { value: "FAST_FOOD", label: "فست‌فود" },
              ]}
            />
          </div>
          {businessResults}
      </section>
    </div>
  );
}
