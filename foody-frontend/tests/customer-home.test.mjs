import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const [source, discover, api, ownerOnboarding, ownerProfile] = await Promise.all([
  readFile(new URL("../src/features/discover/CustomerHome.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/discover/DiscoverPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/businesses/businessApi.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/features/owner/OwnerRegisterBusinessPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/owner/OwnerProfilePage.tsx", import.meta.url), "utf8"),
]);

test("customer home removes city selection and keeps ranked discovery sections", () => {
  assert.doesNotMatch(source, /city|شهر را انتخاب|کسب‌وکارهای شهر انتخابی/i);
  assert.match(source, /filteredFeatured\.length > 0/);
  assert.match(source, /کسب‌وکارهای پیشنهادی/);
  assert.match(source, /filteredTopRated\.length > 0/);
  assert.match(source, /کسب‌وکارهای برتر/);
  assert.match(source, /showTopProducts/);
  assert.match(source, /محصولات برتر/);
});

test("city selection and city API behavior are absent from discovery and owner forms", () => {
  for (const file of [api, discover, ownerOnboarding, ownerProfile]) {
    assert.doesNotMatch(file, /businesses\/cities|businesses\/by-city|businessApi\.cities|businessApi\.byCity/);
  }
  assert.doesNotMatch(ownerOnboarding, /label="شهر"|city:/);
  assert.doesNotMatch(ownerProfile, /label="شهر"|city:/);
});

test("search is debounced, server-backed, grouped, and restores discovery when empty", () => {
  assert.match(discover, /setDebouncedSearch\(search\.trim\(\)\)/);
  assert.match(discover, /businessApi\.discover/);
  assert.match(discover, /productApi\.search\(debouncedSearch\)/);
  assert.match(discover, /showDiscoveryContent=\{!searchActive\}/);
  assert.match(discover, /کسب‌وکارها/);
  assert.match(discover, /محصولات/);
});

test("customer home links to the wallet overview and shows the real aggregate balance", () => {
  assert.doesNotMatch(source, /MOCK_CREDIT_LIMIT|MOCK_APP_DISCOUNT_PERCENT|MOCK_MISSIONS/);
  assert.match(source, /کیف پول‌های شما/);
  assert.match(source, /navigate\("\/wallet"\)/);
  assert.match(source, /wallets\.reduce\(\(sum, w\) => sum \+ Number\(w\.balance\), 0\)/);
  assert.match(source, /formatToman\(totalBalance\)/);
});
