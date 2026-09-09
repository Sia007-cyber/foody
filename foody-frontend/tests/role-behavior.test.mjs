import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const app = await readFile(new URL("../src/App.tsx", import.meta.url), "utf8");
const detail = await readFile(new URL("../src/features/business-detail/BusinessDetailPage.tsx", import.meta.url), "utf8");
const reviews = await readFile(new URL("../src/features/business-detail/ReviewsSection.tsx", import.meta.url), "utf8");
const offers = await readFile(new URL("../src/features/offers/OffersPage.tsx", import.meta.url), "utf8");
const nav = await readFile(new URL("../src/components/PublicNav.tsx", import.meta.url), "utf8");

test("business owners retain marketplace routes and customer history", () => {
  assert.match(app, /roles=\{\["CUSTOMER", "BUSINESS_OWNER"\]\}/);
  assert.match(nav, /کشـف|کشف کسب‌وکارها/);
  assert.match(nav, /user\?\.role === "BUSINESS_OWNER"/);
});

test("customer actions are enabled for customers and owners viewing another business", () => {
  assert.match(detail, /user\?\.role === "CUSTOMER"/);
  assert.match(detail, /business\.ownerUserId !== user\.id/);
  assert.match(detail, /canOrder=\{canUseCustomerActions\}/);
  assert.match(reviews, /user\.id !== ownerUserId/);
});

test("owner self-dealing controls are hidden", () => {
  assert.match(detail, /canUseCustomerActions && <Link/);
  assert.match(detail, /canUseCustomerActions && <CartPanel/);
  assert.match(offers, /ownerBusinessQuery\.data\.id !== offer\.businessId/);
});

test("admin can browse but receives no customer transaction controls", () => {
  assert.match(nav, /user\?\.role === "ADMIN"/);
  assert.match(nav, /to="\/admin"/);
  assert.doesNotMatch(detail, /user\?\.role === "ADMIN".*canUseCustomerActions/);
  assert.match(reviews, /user\?\.role === "ADMIN".*حذف توسط مدیر/);
  assert.match(offers, /const isCustomerActor = user\?\.role === "CUSTOMER" \|\| user\?\.role === "BUSINESS_OWNER"/);
});
