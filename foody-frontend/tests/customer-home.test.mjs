import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("../src/features/discover/CustomerHome.tsx", import.meta.url), "utf8");

test("customer home uses a neutral discovery label for unranked business results", () => {
  assert.match(source, /کافه‌ها را کشف کنید/);
  assert.doesNotMatch(source, /کافه‌های محبوب نزدیک شما/);
});

test("customer home links to the per-business wallet overview without a global balance", () => {
  assert.doesNotMatch(source, /MOCK_CREDIT_LIMIT|MOCK_APP_DISCOUNT_PERCENT|MOCK_MISSIONS/);
  assert.match(source, /کیف پول‌های شما/);
  assert.match(source, /navigate\("\/wallet"\)/);
  assert.doesNotMatch(source, /getBalance|wallet\?\.balance|formatToman/);
});
