import { apiRequest } from "../../lib/api";
import type {
  AdminOrder,
  Business,
  BusinessStatus,
  DashboardSummary,
  OrderStatus,
  User,
  UserRole,
  UserStatus,
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

  orders: (status?: OrderStatus, businessId?: number) =>
    apiRequest<AdminOrder[]>("/api/admin/orders", { query: { status, businessId } }),

  dashboardSummary: () => apiRequest<DashboardSummary>("/api/admin/dashboard/summary"),
};
