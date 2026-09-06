import { createContext, Fragment, useContext, useEffect, useSyncExternalStore, type ReactNode } from "react";
import type { LoginPayload, RegisterPayload } from "./authApi";
import { login, register, logout, startAuthSession } from "./authSession";
import { captureSession, getSessionSnapshot, subscribeSession, updateSessionUser } from "../../lib/session";
import type { User } from "../../types/api";

interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  login: (payload: LoginPayload) => Promise<User>;
  register: (payload: RegisterPayload) => Promise<User>;
  logout: () => Promise<void>;
  updateUser: (user: User) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const session = useSyncExternalStore(subscribeSession, getSessionSnapshot);
  useEffect(startAuthSession, []);

  function updateUser(user: User) {
    const ticket = captureSession();
    // A profile callback from an unmounted account must not restore that user.
    if (ticket.generation === session.generation) updateSessionUser(user, ticket);
  }

  return (
    <AuthContext.Provider value={{ user: session.user, isLoading: session.isLoading, login, register, logout, updateUser }}>
      {/* Drop observers and component-local profile/form state at a boundary. */}
      <Fragment key={session.generation}>{children}</Fragment>
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
