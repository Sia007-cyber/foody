import {
  assertSession, captureSession, getAccessToken, getRefreshToken,
  invalidateSession, refreshSessionTokens, type SessionTicket, type Tokens,
} from "./session.ts";

export { getAccessToken, getRefreshToken } from "./session.ts";
export const BASE_URL = import.meta.env?.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  code: string;
  details: string[] | null;

  constructor(body: unknown, status: number) {
    const data = body !== null && typeof body === "object" ? body as Record<string, unknown> : {};
    const message = typeof data.message === "string" && data.message.trim() ? data.message : null;
    const plainText = typeof body === "string" && body.trim() ? body.trim().slice(0, 500) : null;
    super(message ?? plainText ?? `خطا در ارتباط با سرور (${status})`);
    this.name = "ApiError";
    this.status = status;
    this.code = typeof data.code === "string" ? data.code : "UNKNOWN";
    this.details = Array.isArray(data.details) ? data.details.filter((item): item is string => typeof item === "string") : null;
  }
}

async function responseData(res: Response): Promise<unknown> {
  if (res.status === 204) return undefined;
  let text: string;
  try { text = await res.text(); } catch (error) {
    if (!res.ok) throw new ApiError(null, res.status);
    throw error;
  }
  let data: unknown = null;
  if (text.trim()) {
    try { data = JSON.parse(text); } catch { data = text; }
  }
  if (!res.ok) throw new ApiError(data, res.status);
  return data;
}

export function requireTokens(data: unknown): Tokens {
  const value = data as Partial<Tokens> | null;
  if (!value || typeof value.accessToken !== "string" || !value.accessToken ||
      typeof value.refreshToken !== "string" || !value.refreshToken) {
    throw new ApiError({ message: "پاسخ ورود سرور معتبر نیست" }, 502);
  }
  return { accessToken: value.accessToken, refreshToken: value.refreshToken };
}

let refreshFlight: { generation: number; promise: Promise<boolean> } | null = null;

function tryRefresh(ticket: SessionTicket): Promise<boolean> {
  assertSession(ticket);
  if (refreshFlight?.generation === ticket.generation) return refreshFlight.promise;
  const flight = { generation: ticket.generation, promise: Promise.resolve(false) };
  flight.promise = (async () => {
    try {
      const refreshToken = getRefreshToken();
      if (!refreshToken) throw new ApiError(null, 401);
      const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }), signal: ticket.signal,
      });
      const data = await responseData(res);
      refreshSessionTokens(requireTokens(data), ticket);
      return true;
    } catch {
      invalidateSession(ticket);
      return false;
    } finally {
      if (refreshFlight === flight) refreshFlight = null;
    }
  })();
  refreshFlight = flight;
  return flight.promise;
}

interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "DELETE";
  body?: unknown;
  auth?: boolean;
  query?: Record<string, string | number | undefined | null>;
  // Explicit credentials are for preparing login/register and best-effort logout.
  // They never refresh or modify the current session.
  bearerToken?: string | null;
  signal?: AbortSignal;
}

function buildUrl(path: string, query?: RequestOptions["query"]): string {
  const url = new URL(`${BASE_URL}${path}`);
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null && value !== "") url.searchParams.set(key, String(value));
  }
  return url.toString();
}

/** One retry; delayed 401s using the previous access token reuse the refreshed token. */
async function authenticatedRequest<T>(send: (token: string | null, signal: AbortSignal) => Promise<Response>, refreshAllowed: boolean): Promise<T> {
  const ticket = captureSession();
  const token = getAccessToken();
  async function attempt(accessToken: string | null) {
    assertSession(ticket);
    try {
      const res = await send(accessToken, ticket.signal);
      const data = await responseData(res);
      assertSession(ticket);
      return data as T;
    } catch (error) {
      assertSession(ticket);
      throw error;
    }
  }
  try {
    return await attempt(token);
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401 || !refreshAllowed) throw error;
    if (token === getAccessToken() && !await tryRefresh(ticket)) throw error;
    assertSession(ticket);
    try {
      return await attempt(getAccessToken());
    } catch (retryError) {
      if (retryError instanceof ApiError && retryError.status === 401) invalidateSession(ticket);
      throw retryError;
    }
  }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const send = (token: string | null, signal?: AbortSignal) => {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    if (token) headers.Authorization = `Bearer ${token}`;
    return fetch(buildUrl(path, options.query), {
      method: options.method ?? "GET", headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
      signal,
    });
  };
  if (options.auth === false) {
    const res = await send(options.bearerToken ?? null, options.signal);
    return await responseData(res) as T;
  }
  return authenticatedRequest<T>(send, !path.startsWith("/api/auth/"));
}

/** Multipart uploads share the same normalization, cancellation and refresh lifecycle. */
export async function apiUpload<T>(path: string, file: File): Promise<T> {
  return authenticatedRequest<T>((token, signal) => {
    const headers: Record<string, string> = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    const body = new FormData();
    body.append("file", file);
    return fetch(buildUrl(path), { method: "POST", headers, body, signal });
  }, true);
}

/** Resolves a possibly-relative media path (e.g. "/uploads/x.jpg" from the backend)
 *  into an absolute URL against the API origin — needed because in dev the frontend
 *  (:5173) and backend (:8080) are different origins. Absolute URLs pass through as-is. */
export function resolveMediaUrl(path: string | null | undefined): string | null {
  if (!path) return null;
  if (/^https?:\/\//i.test(path)) return path;
  return `${BASE_URL}${path.startsWith("/") ? "" : "/"}${path}`;
}
