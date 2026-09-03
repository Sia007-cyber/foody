import type { ApiErrorBody } from "../types/api";

export const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

const ACCESS_TOKEN_KEY = "foody.accessToken";
const REFRESH_TOKEN_KEY = "foody.refreshToken";

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export class ApiError extends Error {
  status: number;
  code: string;
  details: string[] | null;

  constructor(body: ApiErrorBody, status: number) {
    super(body.message ?? "خطای ناشناخته رخ داد");
    this.status = status;
    this.code = body.code ?? "UNKNOWN";
    this.details = body.details ?? null;
  }
}

let refreshPromise: Promise<boolean> | null = null;

async function tryRefresh(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;

  if (!refreshPromise) {
    refreshPromise = fetch(`${BASE_URL}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    })
      .then(async (res) => {
        if (!res.ok) return false;
        const data = await res.json();
        setTokens(data.accessToken, data.refreshToken);
        return true;
      })
      .catch(() => false)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "DELETE";
  body?: unknown;
  auth?: boolean; // defaults to true; set false for public endpoints called before login
  query?: Record<string, string | number | undefined | null>;
}

function buildUrl(path: string, query?: RequestOptions["query"]): string {
  const url = new URL(`${BASE_URL}${path}`);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }
  return url.toString();
}

async function rawRequest<T>(path: string, options: RequestOptions): Promise<T> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (options.auth !== false) {
    const token = getAccessToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(buildUrl(path, options.query), {
    method: options.method ?? "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (res.status === 204) return undefined as T;

  const isJson = res.headers.get("content-type")?.includes("application/json");
  const data = isJson ? await res.json() : null;

  if (!res.ok) {
    throw new ApiError(data as ApiErrorBody, res.status);
  }
  return data as T;
}

/** Core request helper: retries once after a silent refresh on 401 (except for auth endpoints themselves). */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  try {
    return await rawRequest<T>(path, options);
  } catch (err) {
    const isAuthRoute = path.startsWith("/api/auth/");
    if (err instanceof ApiError && err.status === 401 && !isAuthRoute && options.auth !== false) {
      const refreshed = await tryRefresh();
      if (refreshed) {
        return await rawRequest<T>(path, options);
      }
      clearTokens();
    }
    throw err;
  }
}

/** Uploads a single image file (multipart/form-data) — used for profile pictures, etc.
 *  Kept separate from apiRequest since it must NOT set Content-Type: application/json
 *  (the browser sets the multipart boundary itself). Still shares the 401-refresh-retry
 *  behavior so an expired access token doesn't surface as a raw failure to the user. */
export async function apiUpload<T>(path: string, file: File): Promise<T> {
  async function send(): Promise<Response> {
    const headers: Record<string, string> = {};
    const token = getAccessToken();
    if (token) headers.Authorization = `Bearer ${token}`;
    const formData = new FormData();
    formData.append("file", file);
    return fetch(buildUrl(path), { method: "POST", headers, body: formData });
  }

  let res = await send();
  if (res.status === 401) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      res = await send();
    }
  }

  const isJson = res.headers.get("content-type")?.includes("application/json");
  const data = isJson ? await res.json() : null;

  if (!res.ok) {
    throw new ApiError(data as ApiErrorBody, res.status);
  }
  return data as T;
}

/** Resolves a possibly-relative media path (e.g. "/uploads/x.jpg" from the backend)
 *  into an absolute URL against the API origin — needed because in dev the frontend
 *  (:5173) and backend (:8080) are different origins. Absolute URLs pass through as-is. */
export function resolveMediaUrl(path: string | null | undefined): string | null {
  if (!path) return null;
  if (/^https?:\/\//i.test(path)) return path;
  return `${BASE_URL}${path.startsWith("/") ? "" : "/"}${path}`;
}
