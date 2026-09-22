import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("../src/features/discover/CustomerHome.tsx", import.meta.url), "utf8");

test("customer home has curated featured and popular sections and hides each empty section", () => {
  assert.match(source, /بهترین کافه‌ها و فست‌فودهای بندرعباس/);
  assert.match(source, /featuredBusinesses\.length > 0/);
  assert.match(source, /کسب‌وکارهای ویژه/);
  assert.match(source, /popularBusinesses\.length > 0/);
  assert.match(source, /کسب‌وکارهای محبوب/);
  assert.doesNotMatch(source, /nearbyBusinesses/);
});

test("customer home links to the wallet overview and shows the real aggregate balance", () => {
  assert.doesNotMatch(source, /MOCK_CREDIT_LIMIT|MOCK_APP_DISCOUNT_PERCENT|MOCK_MISSIONS/);
  assert.match(source, /کیف پول‌های شما/);
  assert.match(source, /navigate\("\/wallet"\)/);
  assert.match(source, /wallets\.reduce\(\(sum, w\) => sum \+ Number\(w\.balance\), 0\)/);
  assert.match(source, /formatToman\(totalBalance\)/);
});
