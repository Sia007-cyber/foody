import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const [registerPage, authCss] = await Promise.all([
  readFile(new URL("../src/features/auth/RegisterPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/auth/auth.css", import.meta.url), "utf8"),
]);

test("registration only renders and submits manager national ID for business owners", () => {
  assert.match(registerPage, /role === "BUSINESS_OWNER" && \(/);
  assert.match(registerPage, /label="کد ملی مدیر \/ مالک"/);
  assert.match(registerPage, /managerNationalId: role === "BUSINESS_OWNER" \? trimmedNationalId : undefined/);
  assert.match(registerPage, /if \(opt\.value === "CUSTOMER"\) setManagerNationalId\(""\)/);
});

test("registration labels email as optional for every role", () => {
  assert.match(registerPage, /label="ایمیل \(اختیاری\)"/);
  assert.match(registerPage, /email: email\.trim\(\) \|\| undefined/);
});

test("registration has compact, responsive card and role controls", () => {
  assert.match(registerPage, /auth-card auth-card-register/);
  assert.match(authCss, /\.auth-card-register \{[\s\S]*max-width: 380px/);
  assert.match(authCss, /\.auth-card-register \.input \{[\s\S]*padding-block: 10px/);
  assert.match(registerPage, /id="registration-phone"/);
  assert.match(registerPage, /helperClassName="registration-helper"/);
  assert.match(authCss, /\.registration-helper \{[\s\S]*color: var\(--ink-soft\)[\s\S]*font-size: 12px/);
  assert.match(authCss, /\.auth-card-register \.role-option \{[\s\S]*padding: 12px 8px/);
  assert.match(authCss, /@media \(max-width: 520px\) \{[\s\S]*\.auth-card-register/);
});
