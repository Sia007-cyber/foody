import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const [discover, api, adminPage, adminApi] = await Promise.all([
  readFile(new URL("../src/features/discover/DiscoverPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/businesses/businessApi.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/features/admin/AdminBusinessesPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/admin/adminApi.ts", import.meta.url), "utf8"),
]);

test("homepage uses dedicated curated queries while keeping the full directory", () => {
  assert.match(api, /\/api\/businesses\/featured/);
  assert.match(api, /\/api\/businesses\/by-city/);
  assert.match(api, /\/api\/businesses\/top-rated/);
  assert.match(discover, /queryKey: \["businesses", "featured"\]/);
  assert.match(discover, /queryKey: \["businesses", "top-rated"\]/);
  assert.match(discover, /id="all-businesses"/);
  assert.match(discover, /همه‌ی کسب‌وکارها/);
});

test("admin business management controls the recommended homepage flag only", () => {
  assert.match(adminApi, /\/api\/admin\/businesses\/\$\{id\}\/featured/);
  assert.match(adminPage, /نمایش در ویژه‌ها/);
  assert.doesNotMatch(adminApi, /\/popular/);
  assert.match(adminPage, /b\.status === "APPROVED"/);
});
