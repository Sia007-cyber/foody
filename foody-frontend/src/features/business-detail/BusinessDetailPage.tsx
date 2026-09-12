import { useParams, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { businessApi } from "../businesses/businessApi";
import { menuApi } from "../catalog/catalogApi";
import { MenuSection } from "./MenuSection";
import { ReviewsSection } from "./ReviewsSection";
import { PageSpinner, EmptyState, ErrorState } from "../../components/Controls";
import "./business-detail.css";
import { useAuth } from "../auth/AuthContext";

const typeLabel: Record<string, string> = { CAFE: "کافه", FAST_FOOD: "فست‌فود" };

export function BusinessDetailPage() {
  const { id } = useParams<{ id: string }>();
  const businessId = Number(id);
  const { user } = useAuth();

  const {
    data: business,
    isLoading: businessLoading,
    isError: businessIsError,
    error: businessError,
    refetch: refetchBusiness,
  } = useQuery({
    queryKey: ["businesses", businessId],
    queryFn: () => businessApi.getById(businessId),
    enabled: Number.isFinite(businessId),
  });

  const { data: menus, isLoading: menusLoading } = useQuery({
    queryKey: ["menus", businessId],
    queryFn: () => menuApi.listForBusiness(businessId),
    enabled: Number.isFinite(businessId),
  });

  if (businessLoading) return <PageSpinner />;
  if (businessIsError)
    return <ErrorState error={businessError} onRetry={() => refetchBusiness()} title="کسب‌وکار لود نشد" />;
  if (!business) return <EmptyState title="کسب‌وکار پیدا نشد" />;
  const canUseCustomerActions = user?.role === "CUSTOMER"
    || (user?.role === "BUSINESS_OWNER" && business.ownerUserId !== user.id);

  return (
    <div>
      <div className="biz-header">
        <h1>{business.name}</h1>
        <div className="biz-header-meta">
          <span>{typeLabel[business.businessType] ?? business.businessType}</span>
          {business.address && <span>{business.address}</span>}
          {business.phone && <span dir="ltr">{business.phone}</span>}
        </div>
        {business.description && <p className="biz-header-desc">{business.description}</p>}
        {canUseCustomerActions && <Link to={`/businesses/${business.id}/reserve`} className="btn btn-secondary btn-sm">رزرو میز</Link>}
      </div>

      <ReviewsSection businessId={business.id} ownerUserId={business.ownerUserId} />

      <div className="container biz-layout">
        <div>
          {menusLoading ? (
            <PageSpinner />
          ) : menus && menus.length > 0 ? (
            menus.map((menu) => <MenuSection key={menu.id} menu={menu} canReview={canUseCustomerActions} />)
          ) : (
            <EmptyState title="این کسب‌وکار هنوز منویی ثبت نکرده" />
          )}
        </div>
      </div>
    </div>
  );
}
