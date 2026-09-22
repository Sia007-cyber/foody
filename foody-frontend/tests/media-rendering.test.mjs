import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const menu = await readFile(new URL("../src/features/business-detail/MenuSection.tsx", import.meta.url), "utf8");
const card = await readFile(new URL("../src/features/discover/BusinessCard.tsx", import.meta.url), "utf8");
const detail = await readFile(new URL("../src/features/business-detail/BusinessDetailPage.tsx", import.meta.url), "utf8");
const owner = await readFile(new URL("../src/features/owner/OwnerProfilePage.tsx", import.meta.url), "utf8");

test("customer catalog renders persisted product and business media URLs", () => {
  assert.match(menu, /resolveMediaUrl\(product\.imageUrl\)/);
  assert.match(card, /resolveMediaUrl\(business\.coverImageUrl\)/);
  assert.match(detail, /resolveMediaUrl\(business\.coverImageUrl\)/);
});

test("cover upload immediately installs the persisted response in the profile cache", () => {
  assert.match(owner, /const updated = await businessApi\.uploadCoverImage\(file\)/);
  assert.match(owner, /setQueryData\(\["business", "profile"\], updated\)/);
  assert.match(owner, /invalidateQueries\(\{ queryKey: \["businesses"\] \}\)/);
});
