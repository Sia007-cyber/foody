import { apiRequest, apiUpload } from "../../lib/api";
import type { Business } from "../../types/api";

export interface UpdateBusinessProfilePayload {
  name?: string;
  description?: string;
  address?: string;
  city?: string;
  latitude?: number;
  longitude?: number;
  phone?: string;
  coverImageUrl?: string;
}

export interface CreateBusinessPayload {
  name: string;
  businessType: string;
  managerNationalId: string;
  description?: string;
  address?: string;
  city?: string;
  phone?: string;
}

export const businessApi = {
  discover: (params: { type?: string; search?: string }) =>
    apiRequest<Business[]>("/api/businesses", { auth: false, query: params }),

  featured: () => apiRequest<Business[]>("/api/businesses/featured", { auth: false }),

  cities: () => apiRequest<string[]>("/api/businesses/cities", { auth: false }),

  byCity: (city: string) => apiRequest<Business[]>("/api/businesses/by-city", { auth: false, query: { city } }),

  topRated: () => apiRequest<Business[]>("/api/businesses/top-rated", { auth: false }),

  getById: (id: number) => apiRequest<Business>(`/api/businesses/${id}`, { auth: false }),

  myProfile: () => apiRequest<Business>("/api/business/profile"),

  updateMyProfile: (payload: UpdateBusinessProfilePayload) =>
    apiRequest<Business>("/api/business/profile", { method: "PATCH", body: payload }),

  uploadCoverImage: (file: File) => apiUpload<Business>("/api/business/profile/cover-image", file),

  // Owner onboarding — one-time self-registration (see backend
  // BusinessOwnerController#create). Throws ApiError with status 409 if the owner
  // already has a business.
  createMyBusiness: (payload: CreateBusinessPayload) =>
    apiRequest<Business>("/api/business", { method: "POST", body: payload }),
};
