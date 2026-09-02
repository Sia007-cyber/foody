package com.foody.wallet.dto;

import com.foody.wallet.entity.Wallet;
import java.math.BigDecimal;

/** Public view of a wallet — just the balance, never exposes the internal wallet id. */
public record WalletResponse(BigDecimal balance) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getBalance());
    }
}
