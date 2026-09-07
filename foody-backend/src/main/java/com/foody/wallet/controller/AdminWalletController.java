package com.foody.wallet.controller;
import com.foody.auth.security.FoodyUserPrincipal; import com.foody.wallet.dto.*; import com.foody.wallet.service.WalletService; import jakarta.validation.Valid; import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/wallets") @PreAuthorize("hasRole('ADMIN')")
public class AdminWalletController {private final WalletService service;public AdminWalletController(WalletService s){service=s;}
 @GetMapping public List<WalletResponse> wallets(){return service.adminWallets();}
 @GetMapping("/{walletId}/transactions") public List<WalletTransactionResponse> tx(@PathVariable Long walletId){return service.adminTransactions(walletId);}
 @PostMapping("/businesses/{businessId}/customers/{customerId}/credit") public WalletResponse credit(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable Long businessId,@PathVariable Long customerId,@Valid @RequestBody AmountRequest r){return service.adminCredit(p.getUserId(),customerId,businessId,r.amount());}
 @PostMapping("/businesses/{businessId}/customers/{customerId}/debit") public WalletResponse debit(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable Long businessId,@PathVariable Long customerId,@Valid @RequestBody AmountRequest r){return service.adminDebit(p.getUserId(),customerId,businessId,r.amount());}}
