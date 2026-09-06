import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("../src/features/admin/AdminDashboardPage.tsx", import.meta.url), "utf8");

test("admin dashboard does not render mock sections and labels summary totals accurately", () => {
  assert.match(source, /تمام دوران/);
  assert.doesNotMatch(source, /RECENT_ACTIVITY|RECENT_REVIEWS|OPEN_VIOLATIONS/);
  assert.doesNotMatch(source, /آخرین نظرات کاربران|گزارش‌های تخلف باز|آخرین فعالیت‌ها/);
});
