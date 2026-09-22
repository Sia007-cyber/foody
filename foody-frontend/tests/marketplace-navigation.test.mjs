import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const discover = await readFile(new URL("../src/features/discover/DiscoverPage.tsx", import.meta.url), "utf8");
const home = await readFile(new URL("../src/features/discover/CustomerHome.tsx", import.meta.url), "utf8");
const nav = await readFile(new URL("../src/components/PublicNav.tsx", import.meta.url), "utf8");
const app = await readFile(new URL("../src/App.tsx", import.meta.url), "utf8");
const detail = await readFile(new URL("../src/features/business-detail/BusinessDetailPage.tsx", import.meta.url), "utf8");
const offers = await readFile(new URL("../src/features/offers/OffersPage.tsx", import.meta.url), "utf8");

test("public and signed-in users share the curated discovery page and components", () => {
  assert.match(discover, /<CustomerHome/);
  assert.match(discover, /featuredBusinesses=\{featuredQuery\.data \?\? \[\]\}/);
  assert.match(discover, /popularBusinesses=\{popularQuery\.data \?\? \[\]\}/);
  assert.doesNotMatch(discover, /user\?\.role === "CUSTOMER"/);
  assert.match(home, /enabled: hasCustomerWallet/);
});

test("public navigation has no owner-management links and retains Business Dashboard access", () => {
  assert.doesNotMatch(nav, /business\/wallets|کیف پول مشتری‌ها/);
  assert.match(nav, /to="\/business"/);
  assert.match(nav, /پنل کسب‌وکار/);
  assert.match(app, /path="\/business\/wallets" element=\{<OwnerWalletPage \/>\}/);
  assert.match(app, /path="\/business\/offers" element=\{<OwnerOffersPage \/>\}/);
});

test("Special Offers is shared with admins but only customer-side roles can claim", () => {
  assert.match(nav, /\{user && <NavLink to="\/offers"/);
  assert.match(offers, /پیشنهادهای ویژه/);
  assert.match(offers, /user\?\.role === "CUSTOMER" \|\| \(user\?\.role === "BUSINESS_OWNER"/);
  assert.doesNotMatch(offers, /user\?\.role === "ADMIN".*canAct/);
});

test("owner customer actions remain unavailable on their own business", () => {
  assert.match(detail, /business\.ownerUserId !== user\.id/);
  assert.match(detail, /canUseCustomerActions && <Link/);
  assert.doesNotMatch(detail, /CartPanel|checkout|سبد خرید/);
});
