import { apiRequest } from "../../lib/api";
import type { Wallet, WalletTransaction } from "../../types/api";

export const walletApi = {
  getBalance: () => apiRequest<Wallet>("/api/wallet"),

  getTransactions: () => apiRequest<WalletTransaction[]>("/api/wallet/transactions"),

  topUp: (amount: number) =>
    apiRequest<Wallet>("/api/wallet/topup", { method: "POST", body: { amount } }),
};
