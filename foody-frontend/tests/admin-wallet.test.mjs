import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const page = await readFile(new URL("../src/features/admin/AdminWalletPage.tsx", import.meta.url), "utf8");
const api = await readFile(new URL("../src/features/admin/adminWalletApi.ts", import.meta.url), "utf8");
const app = await readFile(new URL("../src/App.tsx", import.meta.url), "utf8");
const nav = await readFile(new URL("../src/features/admin/adminNav.tsx", import.meta.url), "utf8");

test("admin wallet page renders business-scoped wallet rows with customer, business, and balance context", () => {
  assert.match(page, /walletsQuery\.data\.map\(\(wallet\)/);
  assert.match(page, /customer\.fullName/);
  assert.match(page, /business\.name/);
  assert.match(page, /formatToman\(wallet\.balance\)/);
  assert.match(page, /adminApi\.users\("CUSTOMER"\)/);
  assert.match(page, /adminApi\.businesses\(\)/);
});

test("admin wallet credit and immediate debit use their real endpoints", () => {
  assert.match(api, /getWallets: \(\) => apiRequest<Wallet\[]>\("\/api\/admin\/wallets"\)/);
  assert.match(api, /credit: \(businessId: number, customerId: number, amount: string\)[\s\S]*\/api\/admin\/wallets\/businesses\/\$\{businessId\}\/customers\/\$\{customerId\}\/credit/);
  assert.match(api, /debit: \(businessId: number, customerId: number, amount: string\)[\s\S]*\/api\/admin\/wallets\/businesses\/\$\{businessId\}\/customers\/\$\{customerId\}\/debit/);
  assert.match(page, /adminWalletApi\.credit\(wallet\.businessId, wallet\.customerUserId, amount\)/);
  assert.match(page, /adminWalletApi\.debit\(wallet\.businessId, wallet\.customerUserId, amount\)/);
});

test("admin debit is immediate and has no customer approval flow", () => {
  assert.match(page, /این عملیات فوری است و به تأیید مشتری یا درخواست برداشت نیاز ندارد/);
  assert.doesNotMatch(page, /debit-requests|approveDebitRequest|rejectDebitRequest/);
});

test("successful admin wallet mutations invalidate wallets and transaction histories without optimistic updates", () => {
  assert.match(page, /invalidateQueries\(\{ queryKey: ADMIN_WALLETS_QUERY_KEY \}\)/);
  assert.match(page, /invalidateQueries\(\{ queryKey: ADMIN_WALLET_TRANSACTIONS_QUERY_KEY \}\)/);
  assert.doesNotMatch(page, /setQueryData/);
});

test("admin transaction history uses the admin wallet endpoint", () => {
  assert.match(api, /getTransactions: \(walletId: number\)[\s\S]*\/api\/admin\/wallets\/\$\{walletId\}\/transactions/);
  assert.match(page, /adminWalletApi\.getTransactions\(wallet\.id\)/);
});

test("admin wallet management is reachable from the protected route and navigation", () => {
  assert.match(app, /path="\/admin\/wallets" element=\{<AdminWalletPage\s*\/>\}/);
  assert.match(nav, /to: "\/admin\/wallets", label: "کیف پول‌ها"/);
});
