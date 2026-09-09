import { apiRequest, apiUpload } from "../../lib/api";
import type { User } from "../../types/api";

export interface UpdateProfilePayload {
  fullName?: string;
  email?: string;
  phone?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  profileImageUrl?: string;
  password?: string;
}

export const usersApi = {
  me: () => apiRequest<User>("/api/users/me"),

  updateMe: (payload: UpdateProfilePayload) =>
    apiRequest<User>("/api/users/me", { method: "PATCH", body: payload }),

  uploadPhoto: (file: File) => apiUpload<User>("/api/users/me/profile-image", file),
};
