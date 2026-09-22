import { apiRequest } from "../../lib/api";
import type {
  AdminOrder,
  AdminUserDetail,
  AdminBusiness,
  BusinessStatus,
  DashboardSummary,
  OrderStatus,
  User,
  UserRole,
  UserStatus,
  TokenResponse,
  AdminReview,
} from "../../types/api";

export const adminApi = {
  businesses: (status?: BusinessStatus) =>
    apiRequest<AdminBusiness[]>("/api/admin/businesses", { query: { status } }),

  approve: (id: number) => apiRequest<AdminBusiness>(`/api/admin/businesses/${id}/approve`, { method: "PATCH" }),
  reject: (id: number) => apiRequest<AdminBusiness>(`/api/admin/businesses/${id}/reject`, { method: "PATCH" }),
  suspend: (id: number) => apiRequest<AdminBusiness>(`/api/admin/businesses/${id}/suspend`, { method: "PATCH" }),
  setFeatured: ({ id, enabled }: { id: number; enabled: boolean }) =>
    apiRequest<AdminBusiness>(`/api/admin/businesses/${id}/featured`, { method: "PATCH", body: { enabled } }),

  users: (role?: UserRole, status?: UserStatus) =>
    apiRequest<User[]>("/api/admin/users", { query: { role, status } }),
  suspendUser: (id: number) => apiRequest<User>(`/api/admin/users/${id}/suspend`, { method: "PATCH" }),
  activateUser: (id: number) => apiRequest<User>(`/api/admin/users/${id}/activate`, { method: "PATCH" }),
  grantAdmin: (id: number) => apiRequest<User>(`/api/admin/users/${id}/grant-admin`, { method: "PATCH" }),
  revokeAdmin: (id: number) => apiRequest<User>(`/api/admin/users/${id}/revoke-admin`, { method: "PATCH" }),
  user: (id: number) => apiRequest<AdminUserDetail>(`/api/admin/users/${id}`),
  impersonateUser: (id: number) => apiRequest<TokenResponse>(`/api/admin/users/${id}/impersonate`, { method: "POST" }),
  resetPassword: ({ id, newPassword }: { id: number; newPassword: string }) =>
    apiRequest<void>(`/api/admin/users/${id}/password-reset`, { method: "POST", body: { newPassword } }),

  orders: (status?: OrderStatus, businessId?: number) =>
    apiRequest<AdminOrder[]>("/api/admin/orders", { query: { status, businessId } }),

  dashboardSummary: () => apiRequest<DashboardSummary>("/api/admin/dashboard/summary"),
  reviews: (status: AdminReview["moderationStatus"]) => apiRequest<AdminReview[]>("/api/admin/reviews", { query: { status } }),
  approveReview: ({ type, id }: { type: AdminReview["reviewType"]; id: number }) => apiRequest<AdminReview>(`/api/admin/reviews/${type.toLowerCase()}/${id}/approve`, { method: "PATCH" }),
  rejectReview: ({ type, id }: { type: AdminReview["reviewType"]; id: number }) => apiRequest<AdminReview>(`/api/admin/reviews/${type.toLowerCase()}/${id}/reject`, { method: "PATCH" }),
};
