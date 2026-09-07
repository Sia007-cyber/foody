import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const page = await readFile(new URL("../src/features/owner/OwnerOffersPage.tsx", import.meta.url), "utf8");
const api = await readFile(new URL("../src/features/owner/ownerOffersApi.ts", import.meta.url), "utf8");
const nav = await readFile(new URL("../src/features/owner/ownerNav.tsx", import.meta.url), "utf8");
const app = await readFile(new URL("../src/App.tsx", import.meta.url), "utf8");

test("owner offer list renders the backend offer fields and derived state", () => {
  assert.match(page, /offersQuery\.data\.map\(\(offer\)/);
  assert.match(page, /offer\.title/);
  assert.match(page, /offer\.description/);
  assert.match(page, /offer\.claimCount/);
  assert.match(page, /offer\.remainingAvailability/);
  assert.match(page, /formatDateTime\(offer\.startsAt\)/);
  assert.match(page, /formatDateTime\(offer\.expiresAt\)/);
  assert.match(page, /offerPresentationState/);
  assert.match(page, /"upcoming"/);
  assert.match(page, /"expired"/);
  assert.match(page, /"full"/);
  assert.match(page, /"cancelled"/);
});

test("owner offers has a clear empty state", () => {
  assert.match(page, /هنوز پیشنهادی نساخته‌ای/);
  assert.match(page, /<EmptyState/);
});

test("offer creation posts title description capacity and ISO times", () => {
  assert.match(api, /createOffer: \(offer: CreateOfferRequest\)[\s\S]*apiRequest<Offer>\("\/api\/business\/offers", \{ method: "POST", body: offer \}\)/);
  assert.match(page, /onSubmit=\{createMutation\.mutate\}/);
  assert.match(page, /title: title\.trim\(\)/);
  assert.match(page, /description: description\.trim\(\) \|\| undefined/);
  assert.match(page, /startsAt: start\.toISOString\(\)/);
  assert.match(page, /expiresAt: expiry\.toISOString\(\)/);
});

test("offer form validates title capacity and chronological time range before submit", () => {
  assert.match(page, /!title\.trim\(\)/);
  assert.match(page, /parsedCapacity <= 0/);
  assert.match(page, /expiry <= start/);
  assert.match(page, /ظرفیت باید بیشتر از صفر باشد/);
  assert.match(page, /زمان پایان باید بعد از زمان شروع باشد/);
});

test("offer cancellation requires confirmation and uses the authenticated owner endpoint", () => {
  assert.match(page, /<ConfirmDialog/);
  assert.match(page, /offerToCancel/);
  assert.match(page, /cancelMutation\.mutate\(offerToCancel\.id\)/);
  assert.match(api, /cancelOffer: \(id: number\)[\s\S]*\/api\/business\/offers\/\$\{id\}\/cancel[\s\S]*method: "PATCH"/);
  assert.match(page, /mayCancel/);
});

test("successful offer mutations invalidate the offer query and owner navigation exposes the route", () => {
  assert.match(page, /invalidateQueries\(\{ queryKey: OWNER_OFFERS_QUERY_KEY \}\)/);
  assert.match(nav, /to: "\/business\/offers", label: "پیشنهادهای محدود"/);
  assert.match(app, /path="\/business\/offers" element=\{<OwnerOffersPage \/>\}/);
});
