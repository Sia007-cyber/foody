import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const page = await readFile(new URL("../src/features/owner/OwnerWalletPage.tsx", import.meta.url), "utf8");
const api = await readFile(new URL("../src/features/owner/ownerWalletApi.ts", import.meta.url), "utf8");
const picker = await readFile(new URL("../src/features/owner/CustomerPicker.tsx", import.meta.url), "utf8");
const sale = await readFile(new URL("../src/features/owner/OwnerNewSalePage.tsx", import.meta.url), "utf8");

test("owner wallet renders every backend wallet with its returned customer identity and balance", () => {
  assert.match(page, /walletsQuery\.data\.map\(\(wallet\)/);
  assert.match(page, /wallet\.customerDisplayName/);
  assert.match(page, /wallet\.customerPublicId/);
  assert.doesNotMatch(page, /wallet\.customerUserId/);
  assert.match(page, /formatToman\(wallet\.balance\)/);
  assert.match(api, /getWallets: \(\) => apiRequest<OwnerWallet\[]>\("\/api\/business\/wallets"\)/);
});

test("owner credit action uses the authenticated-owner credit endpoint", () => {
  assert.match(api, /creditCustomer: \(publicId: string, amount: string\)[\s\S]*encodeURIComponent\(publicId\)[\s\S]*\/credit/);
  assert.match(page, /ownerWalletApi\.creditCustomer\(wallet\.customerPublicId, amount\)/);
});

test("owner debit action creates a request instead of a direct debit", () => {
  assert.match(api, /createDebitRequest: \(publicId: string, amount: string\)/);
  assert.match(page, /ownerWalletApi\.createDebitRequest\(wallet\.customerPublicId, amount\)/);
  assert.match(page, /ثبت درخواست برداشت/);
  assert.match(page, /تا تأیید مشتری، از موجودی کم نمی‌شود/);
});

test("owner searches a bounded customer picker by name or Foody ID and grants first credit", () => {
  assert.match(api, /searchCustomers: \(query: string, page = 0\)/);
  assert.match(api, /customers\/search\?q=.*page=.*limit=10/);
  assert.match(picker, /نام یا شناسه فودی مشتری/);
  assert.match(picker, /minLength=\{2\}/);
  assert.match(picker, /customer\.displayName/);
  assert.match(picker, /customer\.publicId/);
  assert.match(picker, /role="listbox"/);
  assert.match(page, /foundCustomer\.displayName/);
  assert.match(page, /foundCustomer\.publicId/);
  assert.match(page, /ownerWalletApi\.creditCustomer\(foundCustomer!\.publicId, firstCreditAmount\)/);
  assert.match(page, /افزودن اعتبار/);
});

test("selected customer integrates with debit request and purchase approval flows", () => {
  assert.match(page, /ownerWalletApi\.createDebitRequest\(foundCustomer!\.publicId, debitAmount\)/);
  assert.match(page, /برداشت فقط پس از تأیید مشتری/);
  assert.match(sale, /ownerWalletApi\.createPurchase\(customer!\.publicId/);
  assert.match(sale, /CustomerPicker selected=\{customer\}/);
  assert.match(sale, /درخواست خرید برای تایید مشتری ثبت شد/);
});

test("debit requests have no local balance update and successful actions invalidate owner wallets", () => {
  assert.doesNotMatch(page, /onMutate|setQueryData/);
  assert.match(page, /invalidateQueries\(\{ queryKey: OWNER_WALLETS_QUERY_KEY \}\)/);
});
