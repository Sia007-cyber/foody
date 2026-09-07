import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const page = await readFile(new URL("../src/features/wallet/WalletPage.tsx", import.meta.url), "utf8");
const api = await readFile(new URL("../src/features/wallet/walletApi.ts", import.meta.url), "utf8");

test("customer wallet renders balances and histories per business", () => {
  assert.match(page, /wallets\.map\(\(wallet\)/);
  assert.match(page, /businessApi\.getById\(businessId\)/);
  assert.match(page, /walletApi\.getTransactions\(wallet\.id\)/);
  assert.match(page, /اعتبار هر کافه/);
  assert.match(page, /formatToman\(wallet\.balance\)/);
});

test("customer wallet shows a copyable Foody ID even before a business wallet exists", () => {
  assert.match(page, /const \{ user \} = useAuth\(\)/);
  assert.match(page, /user\?\.publicId && \(/);
  assert.match(page, /شناسه فودی من/);
  assert.match(page, /navigator\.clipboard\.writeText\(user\.publicId\)/);
  assert.match(page, /این شناسه را با کافه به اشتراک بگذارید تا برایتان اعتبار ثبت کند/);
  assert.match(page, /wallets\.length === 0 \? <div className="wallet-empty-card">/);
});

test("customer wallet renders pending debit request context", () => {
  assert.match(page, /getPendingDebitRequests/);
  assert.match(page, /request\.amount/);
  assert.match(page, /REQUEST_STATUS_LABEL\[request\.status\]/);
  assert.match(page, /درخواست‌های برداشت/);
});

test("approve and reject actions call their respective customer wallet APIs", () => {
  assert.match(api, /approveDebitRequest: \(requestId: number\)[\s\S]*\/api\/wallet\/debit-requests\/\$\{requestId\}\/approve/);
  assert.match(api, /rejectDebitRequest: \(requestId: number\)[\s\S]*\/api\/wallet\/debit-requests\/\$\{requestId\}\/reject/);
  assert.match(page, /action === "approve" \? walletApi\.approveDebitRequest\(requestId\) : walletApi\.rejectDebitRequest\(requestId\)/);
  assert.match(page, /action: "approve"/);
  assert.match(page, /action: "reject"/);
});

test("request resolution refreshes wallet balances, transactions, and pending requests after success", () => {
  assert.match(page, /invalidateQueries\(\{ queryKey: \["wallet", "wallets"\] \}\)/);
  assert.match(page, /invalidateQueries\(\{ queryKey: \["wallet", "transactions"\] \}\)/);
  assert.match(page, /invalidateQueries\(\{ queryKey: \["wallet", "debit-requests"\] \}\)/);
  assert.doesNotMatch(page, /onMutate/);
});
