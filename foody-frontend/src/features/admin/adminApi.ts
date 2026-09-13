import { apiRequest } from "../../lib/api";
import type {
  AdminOrder,
  AdminUserDetail,
  Business,
  BusinessStatus,
  DashboardSummary,
  OrderStatus,
  User,
  UserRole,
  UserStatus,
  TokenResponse,
} from "../../types/api";

export const adminApi = {
  businesses: (status?: BusinessStatus) =>
    apiRequest<Business[]>("/api/admin/businesses", { query: { status } }),

  approve: (id: number) => apiRequest<Business>(`/api/admin/businesses/${id}/approve`, { method: "PATCH" }),
  reject: (id: number) => apiRequest<Business>(`/api/admin/businesses/${id}/reject`, { method: "PATCH" }),
  suspend: (id: number) => apiRequest<Business>(`/api/admin/businesses/${id}/suspend`, { method: "PATCH" }),

  users: (role?: UserRole, status?: UserStatus) =>
    apiRequest<User[]>("/api/admin/users", { query: { role, status } }),
  suspendUser: (id: number) => apiRequest<User>(`/api/admin/users/${id}/suspend`, { method: "PATCH" }),
  activateUser: (id: number) => apiRequest<User>(`/api/admin/users/${id}/activate`, { method: "PATCH" }),
  user: (id: number) => apiRequest<AdminUserDetail>(`/api/admin/users/${id}`),
  impersonateUser: (id: number) => apiRequest<TokenResponse>(`/api/admin/users/${id}/impersonate`, { method: "POST" }),
  resetPassword: ({ id, newPassword }: { id: number; newPassword: string }) =>
    apiRequest<void>(`/api/admin/users/${id}/password-reset`, { method: "POST", body: { newPassword } }),

  orders: (status?: OrderStatus, businessId?: number) =>
    apiRequest<AdminOrder[]>("/api/admin/orders", { query: { status, businessId } }),

  dashboardSummary: () => apiRequest<DashboardSummary>("/api/admin/dashboard/summary"),
};
