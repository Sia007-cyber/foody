import { apiRequest } from "../../lib/api";
import type { Business } from "../../types/api";

export interface UpdateBusinessProfilePayload {
  name?: string;
  description?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  phone?: string;
  coverImageUrl?: string;
}

export const businessApi = {
  discover: (params: { type?: string; search?: string }) =>
    apiRequest<Business[]>("/api/businesses", { auth: false, query: params }),

  getById: (id: number) => apiRequest<Business>(`/api/businesses/${id}`, { auth: false }),

  myProfile: () => apiRequest<Business>("/api/business/profile"),

  updateMyProfile: (payload: UpdateBusinessProfilePayload) =>
    apiRequest<Business>("/api/business/profile", { method: "PATCH", body: payload }),
};
