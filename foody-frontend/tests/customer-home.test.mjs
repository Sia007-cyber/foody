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

test("customer home keeps city, recommended, and top-rated sections independent", () => {
  assert.doesNotMatch(source, /بهترین کافه‌ها و فست‌فودهای بندرعباس/);
  assert.match(source, /supportedCities\.map/);
  assert.match(source, /cityBusinesses/);
  assert.match(source, /featuredBusinesses\.length > 0/);
  assert.match(source, /کسب‌وکارهای پیشنهادی/);
  assert.match(source, /topRatedBusinesses\.length > 0/);
  assert.match(source, /کسب‌وکارهای برتر/);
  assert.doesNotMatch(source, /nearbyBusinesses/);
});

test("customer discovery and owner forms share the backend city catalog", () => {
  assert.match(api, /\/api\/businesses\/cities/);
  assert.match(discover, /queryFn: businessApi\.cities/);
  assert.match(ownerOnboarding, /queryFn: businessApi\.cities/);
  assert.match(ownerProfile, /queryFn: businessApi\.cities/);
  assert.doesNotMatch(source, /const IRAN_CITIES/);
});

test("customer home links to the wallet overview and shows the real aggregate balance", () => {
  assert.doesNotMatch(source, /MOCK_CREDIT_LIMIT|MOCK_APP_DISCOUNT_PERCENT|MOCK_MISSIONS/);
  assert.match(source, /کیف پول‌های شما/);
  assert.match(source, /navigate\("\/wallet"\)/);
  assert.match(source, /wallets\.reduce\(\(sum, w\) => sum \+ Number\(w\.balance\), 0\)/);
  assert.match(source, /formatToman\(totalBalance\)/);
});
