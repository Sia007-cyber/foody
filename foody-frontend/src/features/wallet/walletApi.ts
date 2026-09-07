import { apiRequest } from "../../lib/api";
import type { DebitRequest, Wallet, WalletTransaction } from "../../types/api";

export const walletApi = {
  getWallets: () => apiRequest<Wallet[]>("/api/wallet"),

  getTransactions: (walletId: number) => apiRequest<WalletTransaction[]>(`/api/wallet/${walletId}/transactions`),

  getPendingDebitRequests: () => apiRequest<DebitRequest[]>("/api/wallet/debit-requests"),

  approveDebitRequest: (requestId: number) =>
    apiRequest<DebitRequest>(`/api/wallet/debit-requests/${requestId}/approve`, { method: "POST" }),

  rejectDebitRequest: (requestId: number) =>
    apiRequest<DebitRequest>(`/api/wallet/debit-requests/${requestId}/reject`, { method: "POST" }),
};
