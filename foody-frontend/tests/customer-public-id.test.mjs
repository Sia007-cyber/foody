import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const page = await readFile(new URL("../src/features/profile/ProfilePage.tsx", import.meta.url), "utf8");

test("wallet customers see a copyable public Foody ID with sharing guidance", () => {
  assert.match(page, /\(user\.role === "CUSTOMER" \|\| user\.role === "BUSINESS_OWNER"\) && publicId/);
  assert.match(page, /navigator\.clipboard\.writeText\(publicId\)/);
  assert.match(page, /شناسه فودی من/);
  assert.match(page, /برای دریافت اعتبار با کافه به اشتراک بگذارید/);
  assert.match(page, /شناسه فودی کپی شد/);
});
