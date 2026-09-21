import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const vite = readFileSync(new URL("../vite.config.ts", import.meta.url), "utf8");
const html = readFileSync(new URL("../index.html", import.meta.url), "utf8");
const lifecycle = readFileSync(new URL("../src/components/PwaLifecycle.tsx", import.meta.url), "utf8");
const publicNav = readFileSync(new URL("../src/components/PublicNav.tsx", import.meta.url), "utf8");
const dashboardShell = readFileSync(new URL("../src/components/DashboardShell.tsx", import.meta.url), "utf8");

test("PWA manifest is installable and uses the existing Foody identity", () => {
  assert.match(vite, /short_name:\s*['"]فودی['"]/);
  assert.match(vite, /start_url:\s*['"]\/['"]/);
  assert.match(vite, /scope:\s*['"]\/['"]/);
  assert.match(vite, /display:\s*['"]standalone['"]/);
  assert.match(vite, /purpose:\s*['"]maskable['"]/);
  assert.match(html, /name="theme-color"/);
  assert.match(html, /rel="apple-touch-icon"/);
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
