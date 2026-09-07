import { apiRequest } from "../../lib/api";
import type { Wallet, WalletTransaction } from "../../types/api";

/** Admin wallet operations are immediate ledger operations, not owner debit requests. */
export const adminWalletApi = {
  getWallets: () => apiRequest<Wallet[]>("/api/admin/wallets"),

  getTransactions: (walletId: number) =>
    apiRequest<WalletTransaction[]>(`/api/admin/wallets/${walletId}/transactions`),

  credit: (businessId: number, customerId: number, amount: string) =>
    apiRequest<Wallet>(`/api/admin/wallets/businesses/${businessId}/customers/${customerId}/credit`, {
      method: "POST",
      body: { amount },
    }),

  debit: (businessId: number, customerId: number, amount: string) =>
    apiRequest<Wallet>(`/api/admin/wallets/businesses/${businessId}/customers/${customerId}/debit`, {
      method: "POST",
      body: { amount },
    }),
};
