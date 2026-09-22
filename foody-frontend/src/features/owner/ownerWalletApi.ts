import { apiRequest } from "../../lib/api";
import type { CustomerLookup, CustomerSearchResponse, DebitRequest, OwnerWallet, PurchaseHistory } from "../../types/api";

/** Owner wallet endpoints derive the business exclusively from the authenticated owner. */
export const ownerWalletApi = {
  getWallets: () => apiRequest<OwnerWallet[]>("/api/business/wallets"),

  lookupCustomer: (publicId: string) =>
    apiRequest<CustomerLookup>(`/api/business/wallets/customers/${encodeURIComponent(publicId)}`),

  searchCustomers: (query: string, page = 0) =>
    apiRequest<CustomerSearchResponse>(`/api/business/wallets/customers/search?q=${encodeURIComponent(query)}&page=${page}&limit=10`),

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
  createPurchase: (publicId: string, items: { productId: number; quantity: number }[]) =>
    apiRequest<DebitRequest>(`/api/business/wallets/customers/${encodeURIComponent(publicId)}/purchases`, { method: "POST", body: { items } }),
  getSalesHistory: () => apiRequest<PurchaseHistory[]>("/api/business/wallets/sales-history"),
};
