import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const menu = await readFile(new URL("../src/features/business-detail/MenuSection.tsx", import.meta.url), "utf8");
const card = await readFile(new URL("../src/features/discover/BusinessCard.tsx", import.meta.url), "utf8");
const detail = await readFile(new URL("../src/features/business-detail/BusinessDetailPage.tsx", import.meta.url), "utf8");
const owner = await readFile(new URL("../src/features/owner/OwnerProfilePage.tsx", import.meta.url), "utf8");
const productOwner = await readFile(new URL("../src/features/owner/OwnerMenusPage.tsx", import.meta.url), "utf8");
const profile = await readFile(new URL("../src/features/profile/ProfilePage.tsx", import.meta.url), "utf8");

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

test("owner upload controls provide responsive source-image guidance matching accepted formats and limits", () => {
  assert.match(owner, /۱۶۰۰×۹۰۰ پیکسل \(۱۶:۹\)/);
  assert.match(owner, /JPG، PNG یا WebP · حداکثر ۵ مگابایت · نمایش واکنش‌گراست/);
  assert.match(owner, /aria-describedby="business-cover-guidance"/);
  assert.match(productOwner, /۱۲۰۰×۱۲۰۰ پیکسل \(۱:۱\)/);
  assert.match(productOwner, /JPG، PNG یا WebP · حداکثر ۵ مگابایت/);
  assert.match(productOwner, /aria-describedby="product-image-guidance"/);
  assert.match(profile, /۸۰۰×۸۰۰ پیکسل \(۱:۱\)/);
  assert.match(profile, /aria-describedby="profile-image-guidance"/);
});
