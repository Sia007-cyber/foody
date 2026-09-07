import { apiRequest } from "../../lib/api";
import type { DebitRequest, Wallet } from "../../types/api";

/** Owner wallet endpoints derive the business exclusively from the authenticated owner. */
export const ownerWalletApi = {
  getWallets: () => apiRequest<Wallet[]>("/api/business/wallets"),

  creditCustomer: (customerId: number, amount: string) =>
    apiRequest<Wallet>(`/api/business/wallets/customers/${customerId}/credit`, {
      method: "POST",
      body: { amount },
    }),

  createDebitRequest: (customerId: number, amount: string) =>
    apiRequest<DebitRequest>(`/api/business/wallets/customers/${customerId}/debit-requests`, {
      method: "POST",
      body: { amount },
    }),
};
