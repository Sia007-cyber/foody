import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const page = await readFile(new URL("../src/features/owner/OwnerWalletPage.tsx", import.meta.url), "utf8");
const api = await readFile(new URL("../src/features/owner/ownerWalletApi.ts", import.meta.url), "utf8");

test("owner wallet renders every backend wallet with its returned customer identity and balance", () => {
  assert.match(page, /walletsQuery\.data\.map\(\(wallet\)/);
  assert.match(page, /مشتری #\{wallet\.customerUserId\}/);
  assert.match(page, /formatToman\(wallet\.balance\)/);
  assert.match(api, /getWallets: \(\) => apiRequest<Wallet\[]>\("\/api\/business\/wallets"\)/);
});

test("owner credit action uses the authenticated-owner credit endpoint", () => {
  assert.match(api, /creditCustomer: \(customerId: number, amount: string\)[\s\S]*\/api\/business\/wallets\/customers\/\$\{customerId\}\/credit/);
  assert.match(page, /ownerWalletApi\.creditCustomer\(wallet\.customerUserId, amount\)/);
});

test("owner debit action creates a request instead of a direct debit", () => {
  assert.match(api, /createDebitRequest: \(customerId: number, amount: string\)[\s\S]*\/api\/business\/wallets\/customers\/\$\{customerId\}\/debit-requests/);
  assert.match(page, /ownerWalletApi\.createDebitRequest\(wallet\.customerUserId, amount\)/);
  assert.match(page, /ثبت درخواست برداشت/);
  assert.match(page, /تا تأیید مشتری، از موجودی کم نمی‌شود/);
});

test("debit requests have no local balance update and successful actions invalidate owner wallets", () => {
  assert.doesNotMatch(page, /onMutate|setQueryData/);
  assert.match(page, /invalidateQueries\(\{ queryKey: OWNER_WALLETS_QUERY_KEY \}\)/);
});
