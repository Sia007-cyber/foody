import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync, readdirSync } from "node:fs";

const vite = readFileSync(new URL("../vite.config.ts", import.meta.url), "utf8");
const html = readFileSync(new URL("../index.html", import.meta.url), "utf8");
const foodyLogo = readFileSync(new URL("../public/foody-logo.svg", import.meta.url), "utf8");
const publicAssets = readdirSync(new URL("../public/", import.meta.url));
const lifecycle = readFileSync(new URL("../src/components/PwaLifecycle.tsx", import.meta.url), "utf8");
const publicNav = readFileSync(new URL("../src/components/PublicNav.tsx", import.meta.url), "utf8");
const dashboardShell = readFileSync(new URL("../src/components/DashboardShell.tsx", import.meta.url), "utf8");

function pngDimensions(name) {
  const png = readFileSync(new URL(`../public/${name}`, import.meta.url));
  assert.equal(png.subarray(1, 4).toString(), "PNG");
  return [png.readUInt32BE(16), png.readUInt32BE(20)];
}

test("PWA manifest is installable and uses the existing Foody identity", () => {
  assert.match(vite, /short_name:\s*['"]فودی['"]/);
  assert.match(vite, /start_url:\s*['"]\/['"]/);
  assert.match(vite, /scope:\s*['"]\/['"]/);
  assert.match(vite, /display:\s*['"]standalone['"]/);
  assert.match(vite, /purpose:\s*['"]maskable['"]/);
  assert.match(html, /name="theme-color"/);
  assert.match(html, /rel="apple-touch-icon"/);
});

test("browser and PWA icons use the canonical Foody logo instead of legacy or Vite branding", () => {
  assert.match(html, /href="\/foody-logo\.svg"/);
  assert.doesNotMatch(html, /vite/i);
  // The favicon embeds a pre-rasterized, correctly-shaped wordmark as a base64 <image>
  // rather than live <text> + @font-face: the browser's favicon renderer uses a
  // restricted context that does not reliably load embedded fonts, which previously
  // broke Persian letter joining in the tab icon. Embedding a raster image sidesteps
  // that entirely and renders identically everywhere.
  assert.match(foodyLogo, /viewBox="0 0 256 256"/);
  assert.match(foodyLogo, /<image href="data:image\/png;base64,/);
  assert.doesNotMatch(foodyLogo, /@font-face|<text/);
  assert.doesNotMatch(foodyLogo, /#863bff|#7e14ff|#ff6b00|vite/i);
  assert.deepEqual(
    publicAssets.filter((asset) => /wordmark|foody-mark|foody-symbol|favicon\.svg|icons\.svg/i.test(asset)),
    [],
  );
  for (const icon of ["pwa-192x192.png", "pwa-512x512.png", "pwa-maskable-512x512.png"]) {
    assert.match(vite, new RegExp(icon));
  }
  assert.match(html, /href="\/favicon-32x32\.png"/);
  assert.match(html, /href="\/favicon-16x16\.png"/);
  assert.deepEqual(pngDimensions("apple-touch-icon.png"), [180, 180]);
  assert.deepEqual(pngDimensions("pwa-192x192.png"), [192, 192]);
  assert.deepEqual(pngDimensions("pwa-512x512.png"), [512, 512]);
  assert.deepEqual(pngDimensions("pwa-maskable-512x512.png"), [512, 512]);
  assert.deepEqual(pngDimensions("favicon-32x32.png"), [32, 32]);
  assert.deepEqual(pngDimensions("favicon-16x16.png"), [16, 16]);
});

test("service worker caches only built assets and excludes API and upload navigations", () => {
  assert.match(vite, /runtimeCaching:\s*\[\]/);
  assert.match(vite, /navigateFallbackDenylist:/);
  assert.match(vite, /\/api/);
  assert.match(vite, /\/uploads/);
  assert.doesNotMatch(vite, /BackgroundSyncPlugin|NetworkFirst|CacheFirst/);
});

test("PWA lifecycle reports offline state and asks before applying an update", () => {
  assert.match(lifecycle, /window\.addEventListener\("offline"/);
  assert.match(lifecycle, /onNeedRefresh/);
  assert.match(lifecycle, /updateServiceWorker\.current\?\.\(true\)/);
});

test("install controls only appear for a real deferred prompt or the iOS manual path", () => {
  assert.match(lifecycle, /window\.addEventListener\("beforeinstallprompt"/);
  assert.match(lifecycle, /event\.preventDefault\(\)/);
  assert.match(lifecycle, /deferredInstallPrompt\.current = event/);
  assert.match(lifecycle, /available = !installed && \(installAvailable \|\| hasIosManualInstall\)/);
  assert.match(lifecycle, /if \(!install\?\.available\) return null/);
  assert.match(publicNav, /<PwaInstallEntry/);
  assert.match(dashboardShell, /<PwaInstallEntry variant="navigation"/);
});

test("the Chromium install prompt is initiated by a click and consumed after its result", () => {
  assert.match(lifecycle, /onClick=\{\(\) => \{/);
  assert.match(lifecycle, /prompt\s*\.prompt\(\)/);
  assert.match(lifecycle, /deferredInstallPrompt\.current = null/);
  assert.match(lifecycle, /prompt\.userChoice/);
  assert.match(lifecycle, /outcome === "dismissed"/);
  assert.doesNotMatch(lifecycle, /نصب با موفقیت انجام شد/);
});

test("installed applications and iOS/manual paths are handled without misleading controls", () => {
  assert.match(lifecycle, /display-mode: standalone/);
  assert.match(lifecycle, /navigator as Navigator & \{ standalone\?: boolean \}/);
  assert.match(lifecycle, /window\.addEventListener\("appinstalled"/);
  assert.match(lifecycle, /setInstalled\(true\)/);
  assert.match(lifecycle, /iPad\|iPhone\|iPod/);
  assert.match(lifecycle, /افزودن به صفحهٔ اصلی/);
  assert.match(lifecycle, /if \(hasIosManualInstall\) setShowIosInstructions\(true\)/);
});
