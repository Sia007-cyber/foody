import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("../src/features/discover/NearbyBusinessCard.tsx", import.meta.url), "utf8");

test("nearby business cards do not render ID-derived rating, distance, or discount data", () => {
  assert.doesNotMatch(source, /mockRating|mockDistanceKm|mockDiscountPercent/);
  assert.doesNotMatch(source, /nearby-card-(discount|distance|rating)/);
});
