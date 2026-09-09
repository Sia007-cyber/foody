import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const component = await readFile(new URL("../src/features/business-detail/ReviewsSection.tsx", import.meta.url), "utf8");
const api = await readFile(new URL("../src/features/business-detail/reviewApi.ts", import.meta.url), "utf8");
const detail = await readFile(new URL("../src/features/business-detail/BusinessDetailPage.tsx", import.meta.url), "utf8");

test("business detail renders the real review section", () => {
  assert.match(detail, /ReviewsSection businessId=\{business\.id\}/);
  assert.match(component, /reviewApi\.list\(businessId\)/);
  assert.match(component, /reviewerDisplayName/);
});

test("review summary uses backend average and count", () => {
  assert.match(component, /reviews\.data\.averageRating/);
  assert.match(component, /reviews\.data\.reviewCount/);
  assert.match(component, /میانگین/);
});

test("empty review state is truthful", () => {
  assert.match(component, /reviews\.data\.reviews\.length === 0/);
  assert.match(component, /هنوز نظری ثبت نشده/);
});

test("anonymous visitors see a login action and no review form", () => {
  assert.match(component, /!user/);
  assert.match(component, /to="\/login"/);
  assert.match(component, /user\?\.role === "CUSTOMER"/);
});

test("customer creation sends only rating and comment", () => {
  assert.match(api, /create:.*method: "POST"/s);
  assert.match(component, /reviewApi\.create\(businessId, payload\)/);
  assert.doesNotMatch(component, /customerUserId/);
});

test("an existing customer review switches the form to edit mode", () => {
  assert.match(component, /existing \? reviewApi\.update/);
  assert.match(component, /existing \? "ویرایش نظر شما"/);
  assert.match(component, /mine\.data \?\? null/);
});

test("update and delete use the mine endpoints", () => {
  assert.match(api, /update:.*\/mine.*PATCH/s);
  assert.match(api, /remove:.*\/mine.*DELETE/s);
  assert.match(component, /حذف نظر/);
});

test("pending mutations disable duplicate submission", () => {
  assert.match(component, /loading=\{mutation\.isPending\}/);
  assert.match(component, /disabled=\{mutation\.isPending \|\| deleteMutation\.isPending\}/);
});

test("backend errors are surfaced to the customer", () => {
  assert.match(component, /notify\(errorMessage\(error\), "danger"\)/);
  assert.match(component, /role="alert"/);
});

test("create, update, and delete invalidate public and mine queries", () => {
  assert.match(component, /invalidateQueries\(\{ queryKey: reviewsKey\(businessId\) \}\)/);
  assert.match(component, /invalidateQueries\(\{ queryKey: mineKey\(businessId\) \}\)/);
  assert.match(component, /onDone\(\)/);
});

test("rating input is keyboard-accessible and bounded to five stars", () => {
  assert.match(component, /type="radio"/);
  assert.match(component, /\[1, 2, 3, 4, 5\]/);
  assert.match(component, /امتیاز انتخاب‌شده/);
});

test("touched business detail flow contains no fabricated rating data", () => {
  assert.doesNotMatch(detail, /mockRating|fakeRating|mockReview|ratingFromId/);
  assert.doesNotMatch(component, /mockRating|fakeRating|mockReview|ratingFromId/);
});
