import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const page = await readFile(new URL("../src/features/profile/ProfilePage.tsx", import.meta.url), "utf8");

test("customer profile shows a copyable public Foody ID with sharing guidance", () => {
  assert.match(page, /user\.role === "CUSTOMER" && user\.publicId/);
  assert.match(page, /navigator\.clipboard\.writeText\(user\.publicId!/);
  assert.match(page, /شناسه فودی من/);
  assert.match(page, /برای دریافت اعتبار با کافه به اشتراک بگذارید/);
});
