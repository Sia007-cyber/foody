import { Navigate, Outlet } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { businessApi } from "../features/businesses/businessApi";
import { ApiError } from "../lib/api";
import { PageSpinner } from "./Controls";

/**
 * Gate for /business/* routes: a BUSINESS_OWNER must have completed the one-time
 * business-registration step (POST /api/business) before reaching the dashboard.
 * If GET /api/business/profile 404s (no business yet), send them to the
 * registration page instead. Any other fetch error still renders the dashboard
 * shell — the dashboard's own query will surface it properly with a retry.
 */
export function RequireOwnerBusiness() {
  const { data: business, isLoading, isError, error } = useQuery({
    queryKey: ["business", "profile"],
    queryFn: businessApi.myProfile,
    retry: false,
  });

  if (isLoading) return <PageSpinner />;

  if (isError && error instanceof ApiError && error.status === 404) {
    return <Navigate to="/business/register" replace />;
  }

  if (isError && !business) {
    // Non-404 failure (network, 500, ...) — let the dashboard page's own query
    // handle and surface the error rather than silently redirecting.
    return <Outlet />;
  }

  return <Outlet />;
}
