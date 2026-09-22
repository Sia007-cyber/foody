import { useQuery } from "@tanstack/react-query";
import { BusinessCard } from "../discover/BusinessCard";
import { EmptyState, ErrorState, PageSpinner } from "../../components/Controls";
import { favoritesApi } from "./favoritesApi";
import "../discover/discover.css";

export function FavoritesPage() {
  const query = useQuery({ queryKey: ["favorites", "list"], queryFn: favoritesApi.list });
  if (query.isLoading) return <PageSpinner />;
  if (query.isError) return <ErrorState error={query.error} onRetry={() => query.refetch()} title="علاقه‌مندی‌ها لود نشد" />;
  return <section className="container discover-section" aria-labelledby="favorites-title">
    <div className="discover-section-head"><span className="section-eyebrow">برای بعد نگه داشته‌اید</span><h1 id="favorites-title" className="discover-section-title">علاقه‌مندی‌های من</h1></div>
    {query.data?.length ? <div className="business-grid">{query.data.map((business) => <BusinessCard key={business.id} business={business} />)}</div> : <EmptyState title="هنوز علاقه‌مندی‌ای ندارید" description="با انتخاب قلب کنار هر کسب‌وکار، آن را برای دسترسی سریع ذخیره کنید." />}
  </section>;
}
