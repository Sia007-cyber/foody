import { apiRequest } from "../../lib/api.ts";
import type { TokenResponse, User } from "../../types/api";

export interface LoginPayload {
  email: string;
  password: string;
}

/** Roles a person can self-register as. ADMIN accounts are created separately. */
export type RegistrableRole = "CUSTOMER" | "BUSINESS_OWNER";

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  role: RegistrableRole;
}

export const authApi = {
  login: (payload: LoginPayload, signal?: AbortSignal) =>
    apiRequest<TokenResponse>("/api/auth/login", { method: "POST", body: payload, auth: false, signal }),

  register: (payload: RegisterPayload, signal?: AbortSignal) =>
    apiRequest<TokenResponse>("/api/auth/register", { method: "POST", body: payload, auth: false, signal }),

  logout: (accessToken: string | null) =>
    apiRequest<void>("/api/auth/logout", { method: "POST", auth: false, bearerToken: accessToken }),

  me: () => apiRequest<User>("/api/users/me"),

  candidateMe: (accessToken: string, signal: AbortSignal) =>
    apiRequest<User>("/api/users/me", { auth: false, bearerToken: accessToken, signal }),
};
