package com.foody.wallet.controller;
import com.foody.auth.security.FoodyUserPrincipal; import com.foody.wallet.dto.*; import com.foody.wallet.service.WalletService; import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/wallet") @PreAuthorize("hasRole('CUSTOMER')")
public class CustomerWalletController {private final WalletService service; public CustomerWalletController(WalletService s){service=s;}
 @GetMapping public List<WalletResponse> wallets(@AuthenticationPrincipal FoodyUserPrincipal p){return service.customerWallets(p.getUserId());}
 @GetMapping("/{walletId}/transactions") public List<WalletTransactionResponse> transactions(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable Long walletId){return service.customerTransactions(p.getUserId(),walletId);}
 @GetMapping("/debit-requests") public List<DebitRequestResponse> pending(@AuthenticationPrincipal FoodyUserPrincipal p){return service.pendingRequests(p.getUserId());}
 @PostMapping("/debit-requests/{id}/approve") public DebitRequestResponse approve(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable Long id){return service.approve(p.getUserId(),id);}
 @PostMapping("/debit-requests/{id}/reject") public DebitRequestResponse reject(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable Long id){return service.reject(p.getUserId(),id);}}
