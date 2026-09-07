package com.foody.wallet.dto;

import com.foody.users.entity.User;
import com.foody.wallet.entity.Wallet;
import java.math.BigDecimal;

public record OwnerWalletResponse(Long id, Long businessId, BigDecimal balance,
                                  String customerPublicId, String customerDisplayName) {
    public static OwnerWalletResponse from(Wallet wallet, User customer) {
        return new OwnerWalletResponse(wallet.getId(), wallet.getBusinessId(), wallet.getBalance(),
                customer.getPublicId(), customer.getFullName());
    }
}
