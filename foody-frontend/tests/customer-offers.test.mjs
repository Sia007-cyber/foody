import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const page = await readFile(new URL("../src/features/offers/OffersPage.tsx", import.meta.url), "utf8");
const api = await readFile(new URL("../src/features/offers/customerOffersApi.ts", import.meta.url), "utf8");
const nav = await readFile(new URL("../src/components/PublicNav.tsx", import.meta.url), "utf8");
const app = await readFile(new URL("../src/App.tsx", import.meta.url), "utf8");
const profile = await readFile(new URL("../src/features/profile/ProfilePage.tsx", import.meta.url), "utf8");

test("customer offer discovery renders API-backed offer information", () => {
  assert.match(api, /getClaimableOffers: \(\) => apiRequest<Offer\[\]>\("\/api\/offers", \{ auth: false \}\)/);
  assert.match(page, /offersQuery\.data\.map\(\(offer\)/);
  assert.match(page, /offer\.businessName/);
  assert.match(page, /offer\.title/);
  assert.match(page, /offer\.description/);
  assert.match(page, /offer\.remainingAvailability/);
  assert.match(page, /formatDateTime\(offer\.startsAt\)/);
  assert.match(page, /formatDateTime\(offer\.expiresAt\)/);
});

test("customer offers have loading, error, and empty states", () => {
  assert.match(page, /offersQuery\.isLoading/);
  assert.match(page, /offersQuery\.isError/);
  assert.match(page, /فعلاً پیشنهاد قابل دریافتی نیست/);
});

test("claim flow uses the backend endpoint and protects pending or repeated claims", () => {
  assert.match(api, /claimOffer: \(offerId: number\) => apiRequest<OfferClaim>\(`\/api\/offers\/\$\{offerId\}\/claim`, \{ method: "POST" \}\)/);
  assert.match(page, /claimMutation\.isPending/);
  assert.match(page, /claimedOfferIds\.has\(offer\.id\)/);
  assert.match(page, /قبلاً دریافت کرده‌ای/);
  assert.match(page, /notify\(errorMessage\(error\), "danger"\)/);
});

test("successful claims refresh both discovery and claim history queries", () => {
  assert.match(page, /invalidateQueries\(\{ queryKey: CUSTOMER_OFFERS_QUERY_KEY \}\)/);
  assert.match(page, /invalidateQueries\(\{ queryKey: CUSTOMER_CLAIMS_QUERY_KEY \}\)/);
  assert.match(page, /پیشنهاد برایت ثبت شد/);
});

test("my claims renders API-backed history and preserves empty history", () => {
  assert.match(api, /getMyClaims: \(\) => apiRequest<OfferClaim\[\]>\("\/api\/offers\/my-claims"\)/);
  assert.match(page, /ClaimHistory/);
  assert.match(page, /claims\.map\(\(claim\)/);
  assert.match(page, /claim\.claimedAt/);
  assert.match(page, /claim\.remainingAvailability/);
  assert.match(page, /هنوز پیشنهادی دریافت نکرده‌ای/);
  assert.match(page, /پیشنهاد شماره \$\{claim\.offerId\}/);
});

test("Special Offers is the primary marketplace destination and claim history is secondary", () => {
  assert.match(nav, /to="\/offers"/);
  assert.match(nav, /پیشنهادهای ویژه/);
  assert.doesNotMatch(nav, /offers\/my-claims/);
  assert.doesNotMatch(page, /offers-tabs/);
  assert.match(profile, /to="\/offers\/my-claims"/);
  assert.match(nav, /to="\/offers"/);
  assert.match(app, /path="\/offers" element=\{<OffersPage \/>\}/);
  assert.match(app, /path="\/offers\/my-claims" element=\{<OffersPage \/>\}/);
});

test("customer offers do not invent money, discounts, or mock offer data", () => {
  assert.doesNotMatch(page, /percent|discount|coupon|price|wallet|checkout|reservation/i);
  assert.doesNotMatch(page, /MOCK_|fake|fixture|کسب‌وکار فودی|پیشنهاد فودی/i);
});
