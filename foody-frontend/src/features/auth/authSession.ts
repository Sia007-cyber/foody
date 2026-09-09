import { authApi, type LoginPayload, type RegisterPayload } from "./authApi.ts";
import { ApiError, requireTokens } from "../../lib/api.ts";
import {
  assertSession, captureSession, getAccessToken, getRefreshToken, getSessionSnapshot,
  handleSessionStorage, invalidateSession, onSessionBoundary, replaceSession,
  SessionChangedError, setSessionUser, type SessionTicket,
} from "../../lib/session.ts";
import type { User } from "../../types/api.ts";

function requireUser(user: User): User {
  if (!user || typeof user.id !== "number" || typeof user.email !== "string" ||
      !["CUSTOMER", "BUSINESS_OWNER", "ADMIN"].includes(user.role)) {
    throw new ApiError({ message: "پاسخ پروفایل سرور معتبر نیست" }, 502);
  }
  return user;
}

let restoreFlight: { generation: number; promise: Promise<void> } | null = null;
export function restoreSession(): Promise<void> {
  const ticket = captureSession();
  if (restoreFlight?.generation === ticket.generation) return restoreFlight.promise;
  if (!getAccessToken() && !getRefreshToken()) {
    if (getSessionSnapshot().isLoading) invalidateSession(ticket);
    return Promise.resolve();
  }
  const flight = { generation: ticket.generation, promise: Promise.resolve() };
  flight.promise = (async () => {
    try {
      const user = requireUser(await authApi.me());
      setSessionUser(user, ticket);
    } catch {
      invalidateSession(ticket);
    } finally {
      if (restoreFlight === flight) restoreFlight = null;
    }
  })();
  restoreFlight = flight;
  return flight.promise;
}

/** Called by the provider; cleanup also supports React StrictMode's effect replay. */
export function startAuthSession() {
  const unsubscribe = onSessionBoundary((reason) => {
    if (reason === "storage") void restoreSession();
  });
  window.addEventListener("storage", handleSessionStorage);
  void restoreSession();
  return () => {
    unsubscribe();
    window.removeEventListener("storage", handleSessionStorage);
  };
}

let authAttempt = 0;
async function candidateUser(accessToken: string, ticket: SessionTicket): Promise<User> {
  try {
    return requireUser(await authApi.candidateMe(accessToken, ticket.signal));
  } catch (error) {
    assertSession(ticket);
    // A transient /me failure must not discard the old session. Try once more
    // without repeating registration; if it still fails the caller rolls back.
    if (error instanceof TypeError || (error instanceof ApiError && error.status >= 500)) {
      return requireUser(await authApi.candidateMe(accessToken, ticket.signal));
    }
    throw error;
  }
}

async function authenticate(kind: "login" | "register", payload: LoginPayload | RegisterPayload): Promise<User> {
  const ticket = captureSession();
  const attempt = ++authAttempt;
  const response = kind === "login"
    ? await authApi.login(payload, ticket.signal)
    : await authApi.register(payload as RegisterPayload, ticket.signal);
  assertSession(ticket);
  if (attempt !== authAttempt) throw new SessionChangedError();
  const tokens = requireTokens(response);
  let user: User;
  try {
    user = await candidateUser(tokens.accessToken, ticket);
  } catch (error) {
    assertSession(ticket);
    if (kind === "register") {
      throw new Error("حساب ساخته شد اما دریافت پروفایل موفق نبود؛ از صفحهٔ ورود دوباره وارد شوید", { cause: error });
    }
    throw error;
  }
  assertSession(ticket);
  if (attempt !== authAttempt) throw new SessionChangedError();
  // Only now do tokens + user become authoritative. The boundary clears all
  // queries and aborts the previous session before React sees the new account.
  replaceSession(tokens, user, ticket);
  return user;
}

export const login = (payload: LoginPayload) => authenticate("login", payload);
export const register = (payload: RegisterPayload) => authenticate("register", payload);

export function logout(): Promise<void> {
  const ticket = captureSession();
  // Dispatch using the captured token, outside the aborting session. Completion
  // cannot invalidate a later login and cannot trigger a refresh.
  const refreshToken = getRefreshToken();
  const request = refreshToken ? authApi.logout(refreshToken) : Promise.resolve();
  invalidateSession(ticket);
  void request.catch(() => { /* Best-effort server notification. */ });
  // Let existing logout navigation complete now, never after a later login.
  return Promise.resolve();
}
