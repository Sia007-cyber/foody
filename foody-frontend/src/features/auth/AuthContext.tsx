import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { authApi, type LoginPayload, type RegisterPayload } from "./authApi";
import { clearTokens, getAccessToken, setTokens } from "../../lib/api";
import type { User } from "../../types/api";

interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  login: (payload: LoginPayload) => Promise<User>;
  register: (payload: RegisterPayload) => Promise<User>;
  logout: () => Promise<void>;
  /** Updates the in-memory user (e.g. after a profile edit) without a full re-fetch/reload. */
  updateUser: (user: User) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!getAccessToken()) {
      setIsLoading(false);
      return;
    }
    authApi
      .me()
      .then(setUser)
      .catch(() => clearTokens())
      .finally(() => setIsLoading(false));
  }, []);

  async function login(payload: LoginPayload) {
    const tokens = await authApi.login(payload);
    setTokens(tokens.accessToken, tokens.refreshToken);
    const me = await authApi.me();
    setUser(me);
    return me;
  }

  async function register(payload: RegisterPayload) {
    const tokens = await authApi.register(payload);
    setTokens(tokens.accessToken, tokens.refreshToken);
    const me = await authApi.me();
    setUser(me);
    return me;
  }

  async function logout() {
    try {
      await authApi.logout();
    } catch {
      // Best-effort — clear local state regardless of server response.
    }
    clearTokens();
    setUser(null);
    queryClient.clear();
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout, updateUser: setUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
