import type { User } from "../types/api.ts";

const ACCESS_KEY = "foody.accessToken";
const REFRESH_KEY = "foody.refreshToken";
export interface Tokens { accessToken: string; refreshToken: string }
export interface SessionTicket { generation: number; signal: AbortSignal }
type Boundary = "replace" | "invalidate" | "storage";

function readTokens() {
  return { accessToken: localStorage.getItem(ACCESS_KEY), refreshToken: localStorage.getItem(REFRESH_KEY) };
}
let tokens = readTokens();
let controller = new AbortController();
let snapshot = { generation: 0, user: null as User | null, isLoading: true };
const listeners = new Set<() => void>();
const boundaries = new Set<(reason: Boundary) => void>();

export class SessionChangedError extends Error {
  constructor() { super("نشست تغییر کرده است؛ دوباره تلاش کنید"); this.name = "SessionChangedError"; }
}

export const getSessionSnapshot = () => snapshot;
export function subscribeSession(listener: () => void) {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
}
export function onSessionBoundary(listener: (reason: Boundary) => void) {
  boundaries.add(listener);
  return () => { boundaries.delete(listener); };
}
function emit() { listeners.forEach((listener) => listener()); }

function transition(reason: Boundary, user: User | null, isLoading: boolean) {
  const previous = controller;
  controller = new AbortController();
  snapshot = { generation: snapshot.generation + 1, user, isLoading };
  previous.abort();
  // Cache teardown is synchronous and precedes React notification/new-session rendering.
  boundaries.forEach((listener) => listener(reason));
  emit();
}

/** Read actual storage, not event.newValue: queued events can describe obsolete tokens. */
export function syncStoredSession() {
  const stored = readTokens();
  if (stored.accessToken === tokens.accessToken && stored.refreshToken === tokens.refreshToken) return;
  const removed = (tokens.accessToken !== null && stored.accessToken === null) ||
    (tokens.refreshToken !== null && stored.refreshToken === null);
  tokens = stored;
  if (removed) {
    // Removal of either credential is logout, not an invitation to refresh using
    // the remaining token and silently sign this tab back in.
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    tokens = readTokens();
    transition("invalidate", null, false);
  } else {
    transition("storage", null, Boolean(tokens.accessToken || tokens.refreshToken));
  }
}
export function captureSession(): SessionTicket {
  syncStoredSession();
  return { generation: snapshot.generation, signal: controller.signal };
}
export function assertSession(ticket: SessionTicket) {
  syncStoredSession();
  if (ticket.generation !== snapshot.generation) throw new SessionChangedError();
}
export function getAccessToken() { syncStoredSession(); return tokens.accessToken; }
export function getRefreshToken() { syncStoredSession(); return tokens.refreshToken; }
function writeTokens(next: Tokens) {
  localStorage.setItem(ACCESS_KEY, next.accessToken);
  localStorage.setItem(REFRESH_KEY, next.refreshToken);
  tokens = next;
}
export function replaceSession(next: Tokens, user: User, ticket: SessionTicket) {
  assertSession(ticket);
  writeTokens(next);
  transition("replace", user, false);
}
export function refreshSessionTokens(next: Tokens, ticket: SessionTicket) {
  assertSession(ticket);
  writeTokens(next);
}
export function invalidateSession(ticket = captureSession()) {
  syncStoredSession();
  if (ticket.generation !== snapshot.generation) return;
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
  tokens = readTokens();
  transition("invalidate", null, false);
}
export function setSessionUser(user: User, ticket: SessionTicket) {
  assertSession(ticket);
  snapshot = { ...snapshot, user, isLoading: false };
  emit();
}
export function updateSessionUser(user: User, ticket: SessionTicket) {
  assertSession(ticket);
  if (snapshot.user?.id === user.id) setSessionUser(user, ticket);
}
export function handleSessionStorage(event: StorageEvent) {
  if (event.storageArea === localStorage && (event.key === null || event.key === ACCESS_KEY || event.key === REFRESH_KEY)) {
    syncStoredSession();
  }
}
