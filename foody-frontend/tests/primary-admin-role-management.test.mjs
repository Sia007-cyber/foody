import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const list = await readFile(new URL("../src/features/admin/AdminUsersPage.tsx", import.meta.url), "utf8");
const detail = await readFile(new URL("../src/features/admin/AdminUserDetailPage.tsx", import.meta.url), "utf8");
const api = await readFile(new URL("../src/features/admin/adminApi.ts", import.meta.url), "utf8");
const types = await readFile(new URL("../src/types/api.ts", import.meta.url), "utf8");

test("user model and admin lists distinguish primary and ordinary admins", () => {
  assert.match(types, /primaryAdmin: boolean/);
  assert.match(list, /u\.primaryAdmin \? "مدیر اصلی"/);
  assert.match(list, /u\.role === "ADMIN" \? "مدیر عادی"/);
});

test("only the authenticated primary admin sees valid role controls", () => {
  assert.match(detail, /const isPrimaryAdmin = admin\?\.primaryAdmin === true/);
  assert.match(detail, /canGrantAdmin = isPrimaryAdmin && eligible && target\.status === "ACTIVE"/);
  assert.match(detail, /canRevokeAdmin = isPrimaryAdmin && target\.role === "ADMIN" && !target\.primaryAdmin/);
});

test("grant and revoke require confirmations and refresh admin user queries", () => {
  assert.match(detail, /title="اعطای دسترسی مدیر؟"/);
  assert.match(detail, /title="لغو دسترسی مدیر؟"/);
  assert.match(detail, /roleMutation\.mutate\("grant"\)/);
  assert.match(detail, /roleMutation\.mutate\("revoke"\)/);
  assert.match(detail, /invalidateQueries\(\{ queryKey: \["admin", "users"\] \}\)/);
});

test("role API exposes only explicit grant and revoke operations", () => {
  assert.match(api, /\/grant-admin/);
  assert.match(api, /\/revoke-admin/);
  assert.doesNotMatch(api, /role:\s*string/);
});
