package com.foody.wallet.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.wallet.dto.TopUpRequest;
import com.foody.wallet.dto.WalletResponse;
import com.foody.wallet.dto.WalletTransactionResponse;
import com.foody.wallet.service.WalletService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet endpoints for the authenticated user (all roles — customer, business owner,
 * or admin each has their own wallet). Not in WebSecurityConfig's PUBLIC_MATCHERS,
 * so every route here requires a valid JWT.
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletResponse getBalance(@AuthenticationPrincipal FoodyUserPrincipal principal) {
        return walletService.getBalance(principal.getUserId());
    }

    @GetMapping("/transactions")
    public List<WalletTransactionResponse> getTransactions(@AuthenticationPrincipal FoodyUserPrincipal principal) {
        return walletService.getTransactions(principal.getUserId());
    }

    @PostMapping("/topup")
    public WalletResponse topUp(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                @Valid @RequestBody TopUpRequest request) {
        return walletService.topUp(principal.getUserId(), request.amount());
    }
}
