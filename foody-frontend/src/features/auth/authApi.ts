import { apiRequest } from "../../lib/api";
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
  login: (payload: LoginPayload) =>
    apiRequest<TokenResponse>("/api/auth/login", { method: "POST", body: payload, auth: false }),

  register: (payload: RegisterPayload) =>
    apiRequest<TokenResponse>("/api/auth/register", { method: "POST", body: payload, auth: false }),

  logout: () => apiRequest<void>("/api/auth/logout", { method: "POST" }),

  me: () => apiRequest<User>("/api/users/me"),
};
