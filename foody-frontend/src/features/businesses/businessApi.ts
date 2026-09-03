import { apiRequest, apiUpload } from "../../lib/api";
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

export interface CreateBusinessPayload {
  name: string;
  businessType: string;
  description?: string;
  address?: string;
  phone?: string;
}

export const businessApi = {
  discover: (params: { type?: string; search?: string }) =>
    apiRequest<Business[]>("/api/businesses", { auth: false, query: params }),

  getById: (id: number) => apiRequest<Business>(`/api/businesses/${id}`, { auth: false }),

  myProfile: () => apiRequest<Business>("/api/business/profile"),

  updateMyProfile: (payload: UpdateBusinessProfilePayload) =>
    apiRequest<Business>("/api/business/profile", { method: "PATCH", body: payload }),

  /** Uploads a cover photo and returns its public URL; caller still needs to
   *  PATCH /api/business/profile with coverImageUrl to actually attach it. Reuses the
   *  same generic image-upload endpoint the personal profile picture uses. */
  uploadCoverImage: (file: File) => apiUpload<{ url: string }>("/api/uploads/image", file),

  // Owner onboarding — one-time self-registration (see backend
  // BusinessOwnerController#create). Throws ApiError with status 409 if the owner
  // already has a business.
  createMyBusiness: (payload: CreateBusinessPayload) =>
    apiRequest<Business>("/api/business", { method: "POST", body: payload }),
};
