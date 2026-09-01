import { apiRequest } from "../../lib/api";
import type { Business, BusinessStatus, DashboardSummary } from "../../types/api";

export const adminApi = {
  businesses: (status?: BusinessStatus) =>
    apiRequest<Business[]>("/api/admin/businesses", { query: { status } }),

  approve: (id: number) => apiRequest<Business>(`/api/admin/businesses/${id}/approve`, { method: "PATCH" }),
  reject: (id: number) => apiRequest<Business>(`/api/admin/businesses/${id}/reject`, { method: "PATCH" }),
  suspend: (id: number) => apiRequest<Business>(`/api/admin/businesses/${id}/suspend`, { method: "PATCH" }),

  dashboardSummary: () => apiRequest<DashboardSummary>("/api/admin/dashboard/summary"),
};
