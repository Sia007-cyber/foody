import { apiRequest } from "../../lib/api";
import type { CustomerLookup, DebitRequest, OwnerWallet } from "../../types/api";

/** Owner wallet endpoints derive the business exclusively from the authenticated owner. */
export const ownerWalletApi = {
  getWallets: () => apiRequest<OwnerWallet[]>("/api/business/wallets"),

  lookupCustomer: (publicId: string) =>
    apiRequest<CustomerLookup>(`/api/business/wallets/customers/${encodeURIComponent(publicId)}`),

  creditCustomer: (publicId: string, amount: string) =>
    apiRequest<OwnerWallet>(`/api/business/wallets/customers/${encodeURIComponent(publicId)}/credit`, {
      method: "POST",
      body: { amount },
    }),

  createDebitRequest: (publicId: string, amount: string) =>
    apiRequest<DebitRequest>(`/api/business/wallets/customers/${encodeURIComponent(publicId)}/debit-requests`, {
      method: "POST",
      body: { amount },
    }),
};
