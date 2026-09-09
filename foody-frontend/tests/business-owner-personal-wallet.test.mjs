import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const app = await readFile(new URL("../src/App.tsx", import.meta.url), "utf8");
const nav = await readFile(new URL("../src/components/PublicNav.tsx", import.meta.url), "utf8");
const home = await readFile(new URL("../src/features/discover/CustomerHome.tsx", import.meta.url), "utf8");
const detail = await readFile(new URL("../src/features/business-detail/BusinessDetailPage.tsx", import.meta.url), "utf8");
const ownerNav = await readFile(new URL("../src/features/owner/ownerNav.tsx", import.meta.url), "utf8");

test("business owners and customers share the personal wallet route while admins do not", () => {
  assert.match(app, /RequireAuth roles=\{\["CUSTOMER", "BUSINESS_OWNER"\]\}[\s\S]*path="\/wallet"/);
  assert.match(nav, /\(user\?\.role === "CUSTOMER" \|\| user\?\.role === "BUSINESS_OWNER"\)[\s\S]*to="\/wallet"/);
  assert.match(home, /const hasCustomerWallet = user\?\.role === "CUSTOMER" \|\| user\?\.role === "BUSINESS_OWNER"/);
});

test("personal wallet and own-business wallet management remain separate", () => {
  assert.match(nav, /to="\/wallet"[\s\S]*کیف پول من/);
  assert.match(ownerNav, /to: "\/business\/wallets", label: "مدیریت اعتبار"/);
});

test("owner customer actions stay hidden for their own business", () => {
  assert.match(detail, /user\?\.role === "BUSINESS_OWNER" && business\.ownerUserId !== user\.id/);
});
