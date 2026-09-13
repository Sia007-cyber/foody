import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const detail = await readFile(new URL("../src/features/admin/AdminUserDetailPage.tsx", import.meta.url), "utf8");
const banner = await readFile(new URL("../src/components/ImpersonationBanner.tsx", import.meta.url), "utf8");
const session = await readFile(new URL("../src/lib/session.ts", import.meta.url), "utf8");
const css = await readFile(new URL("../src/components/ui.css", import.meta.url), "utf8");

test("login-as action is eligible-role only and requires confirmation", () => {
  assert.match(detail, /target\.role === "CUSTOMER" \|\| target\.role === "BUSINESS_OWNER"/);
  assert.match(detail, /setConfirmImpersonation\(true\)/);
  assert.match(detail, /<ConfirmDialog title="ورود به‌جای کاربر؟"/);
});

test("impersonation is conspicuous, identifies target, and restores admin context", () => {
  assert.match(banner, /حالت پشتیبانی/);
  assert.match(banner, /impersonation\.targetName/);
  assert.match(banner, /خروج از حالت پشتیبانی/);
  assert.match(session, /ADMIN_CONTEXT_KEY/);
  assert.match(session, /restoreAdminSession/);
});

test("password reset exists, validates policy, and requires confirmation", () => {
  assert.match(detail, /admin-reset-password/);
  assert.match(detail, /minLength=\{8\}/);
  assert.match(detail, /setConfirmReset\(true\)/);
  assert.match(detail, /<ConfirmDialog title="بازنشانی رمز عبور؟"/);
});

test("security copy says current credentials cannot be viewed and no old password field exists", () => {
  assert.match(detail, /رمزهای عبور فعلی قابل مشاهده نیستند/);
  assert.doesNotMatch(detail, /oldPassword|passwordHash|refreshToken|JWT secret/i);
});

test("support layout has explicit mobile and RTL-safe logical behavior", () => {
  assert.match(css, /@media \(max-width: 760px\)/);
  assert.match(css, /admin-user-detail-grid[^}]*grid-template-columns: 1fr/);
  assert.match(detail, /dir="ltr"/);
});
