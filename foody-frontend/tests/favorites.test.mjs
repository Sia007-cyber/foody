import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const [api, button, page, card, detail, nav] = await Promise.all([
  readFile(new URL("../src/features/favorites/favoritesApi.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/features/favorites/FavoriteButton.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/favorites/FavoritesPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/discover/BusinessCard.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/business-detail/BusinessDetailPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/components/PublicNav.tsx", import.meta.url), "utf8"),
]);

test("favorites use server APIs and include list/state endpoints", () => {
  assert.match(api, /\/api\/favorites/);
  assert.match(api, /business-ids/);
  assert.match(api, /method: "PUT"/);
  assert.match(api, /method: "DELETE"/);
});

test("heart toggles optimistically and sends anonymous visitors to login", () => {
  assert.match(button, /queryClient\.setQueryData/);
  assert.match(button, /invalidateQueries/);
  assert.match(button, /navigate\("\/login"/);
  assert.match(button, /user\?\.role === "CUSTOMER"/);
  assert.match(button, /aria-label/);
});

test("favorites destination and reusable hearts are present", () => {
  assert.match(page, /هنوز علاقه‌مندی‌ای ندارید/);
  assert.match(page, /BusinessCard/);
  assert.match(card, /FavoriteButton/);
  assert.match(detail, /FavoriteButton/);
  assert.match(nav, /\/favorites/);
});
