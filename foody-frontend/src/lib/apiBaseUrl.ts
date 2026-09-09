export function apiBaseUrl(value: string | undefined, production: boolean): string {
  const configured = value?.trim();
  if (!configured && production) throw new Error("VITE_API_BASE_URL is required for production builds");
  const url = new URL(configured || "http://localhost:8080");
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password ||
      url.pathname !== '/' || url.search || url.hash ||
      (production && (url.protocol !== 'https:' || ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname)))) {
    throw new Error("VITE_API_BASE_URL must be an API origin (HTTPS in production), without a path or credentials");
  }
  return url.origin;
}
