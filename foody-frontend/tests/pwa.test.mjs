import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const vite = readFileSync(new URL("../vite.config.ts", import.meta.url), "utf8");
const html = readFileSync(new URL("../index.html", import.meta.url), "utf8");
const lifecycle = readFileSync(new URL("../src/components/PwaLifecycle.tsx", import.meta.url), "utf8");

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
