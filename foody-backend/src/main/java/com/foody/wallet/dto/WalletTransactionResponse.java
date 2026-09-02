package com.foody.wallet.dto;

import com.foody.wallet.entity.WalletTransaction;
import com.foody.wallet.entity.WalletTransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record WalletTransactionResponse(
        Long id,
        WalletTransactionType type,
        boolean credit,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        String referenceType,
        Long referenceId,
        Instant createdAt) {

    public static WalletTransactionResponse from(WalletTransaction tx) {
        return new WalletTransactionResponse(
                tx.getId(), tx.getType(), tx.isCredit(), tx.getAmount(), tx.getBalanceAfter(),
                tx.getDescription(), tx.getReferenceType(), tx.getReferenceId(), tx.getCreatedAt());
    }
}
