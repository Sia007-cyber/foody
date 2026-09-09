import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { DashboardShell } from "../../components/DashboardShell";
import { EmptyState, ErrorState, PageSpinner } from "../../components/Controls";
import { businessApi } from "../businesses/businessApi";
import { adminNavItems } from "./adminNav";

export function AdminReviewsPage() {
  const businesses = useQuery({
    queryKey: ["businesses", "admin-review-moderation"],
    queryFn: () => businessApi.discover({}),
  });
  return (
    <DashboardShell navItems={adminNavItems} title="مدیریت نظرات">
      {businesses.isLoading ? <PageSpinner /> : businesses.isError ? (
        <ErrorState error={businesses.error} onRetry={() => businesses.refetch()} title="کسب‌وکارها لود نشدند" />
      ) : !businesses.data?.length ? <EmptyState title="کسب‌وکار فعالی پیدا نشد" /> : (
        <div className="list-group">
          {businesses.data.map((business) => (
            <div className="list-row" key={business.id}>
              <div className="list-row-main"><span className="list-row-title">{business.name}</span></div>
              <Link className="btn btn-secondary btn-sm" to={`/businesses/${business.id}`}>مشاهده و مدیریت نظرات</Link>
            </div>
          ))}
        </div>
      )}
    </DashboardShell>
  );
}
