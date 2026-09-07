package com.foody.wallet.service;
import com.foody.wallet.dto.*; import java.math.BigDecimal; import java.util.List;
public interface WalletService {
 List<WalletResponse> customerWallets(Long customerId); List<WalletTransactionResponse> customerTransactions(Long customerId,Long walletId);
 List<DebitRequestResponse> pendingRequests(Long customerId); DebitRequestResponse approve(Long customerId,Long requestId); DebitRequestResponse reject(Long customerId,Long requestId);
 List<WalletResponse> ownerWallets(Long ownerId); WalletResponse ownerCredit(Long ownerId,Long customerId,BigDecimal amount); DebitRequestResponse ownerRequestDebit(Long ownerId,Long customerId,BigDecimal amount);
 WalletResponse adminCredit(Long adminId,Long customerId,Long businessId,BigDecimal amount); WalletResponse adminDebit(Long adminId,Long customerId,Long businessId,BigDecimal amount);
 List<WalletResponse> adminWallets(); List<WalletTransactionResponse> adminTransactions(Long walletId);
}
