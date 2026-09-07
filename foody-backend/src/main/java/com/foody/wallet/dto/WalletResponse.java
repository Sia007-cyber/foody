package com.foody.wallet.dto;
import com.foody.wallet.entity.Wallet; import java.math.BigDecimal;
public record WalletResponse(Long id,Long customerUserId,Long businessId,BigDecimal balance){public static WalletResponse from(Wallet w){return new WalletResponse(w.getId(),w.getCustomerUserId(),w.getBusinessId(),w.getBalance());}}
