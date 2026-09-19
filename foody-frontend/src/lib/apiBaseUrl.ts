export function apiBaseUrl(value: string | undefined, production: boolean): string {
  const configured = value?.trim();
  // Nginx serves the VPS build beside /api and /uploads, so an unset production
  // value intentionally means same-origin. Development keeps its separate API.
  if (!configured) return production ? "" : "http://localhost:8080";
  const url = new URL(configured);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password ||
      url.pathname !== '/' || url.search || url.hash ||
      (production && (url.protocol !== 'https:' || ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname)))) {
    throw new Error("VITE_API_BASE_URL must be an API origin (HTTPS in production), without a path or credentials");
  }
  return url.origin;
}
