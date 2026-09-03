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

  /** Uploads a profile picture and returns its public URL; caller still needs to
   *  PATCH /api/users/me with profileImageUrl to actually attach it to the account. */
  uploadPhoto: (file: File) => apiUpload<{ url: string }>("/api/uploads/image", file),
};
