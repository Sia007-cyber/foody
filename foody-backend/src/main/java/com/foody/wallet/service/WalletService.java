package com.foody.wallet.service;

import com.foody.wallet.dto.WalletResponse;
import com.foody.wallet.dto.WalletTransactionResponse;
import com.foody.wallet.entity.WalletTransactionType;
import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    /** Current balance, creating a zero-balance wallet on first access if none exists yet. */
    WalletResponse getBalance(Long userId);

    List<WalletTransactionResponse> getTransactions(Long userId);

    /** Simulated top-up — no real payment gateway yet (see TOPUP javadoc on the enum). */
    WalletResponse topUp(Long userId, BigDecimal amount);

    /**
     * Debits {@code amount} from the user's wallet for some other module's use (e.g. order
     * checkout, once wallet becomes a payment method). Throws InsufficientBalanceException if
     * the balance is too low. Not exposed directly as a controller endpoint yet — Phase 2 wires
     * a caller into this once the "does wallet replace PAY_ON_DELIVERY" decision is made.
     */
    WalletTransactionResponse debit(Long userId, BigDecimal amount, String referenceType,
                                    Long referenceId, String description);

    /** Credits {@code amount} to the user's wallet — refunds, referral/mission rewards, etc. */
    WalletTransactionResponse credit(Long userId, BigDecimal amount, WalletTransactionType type,
                                     String referenceType, Long referenceId, String description);
}
