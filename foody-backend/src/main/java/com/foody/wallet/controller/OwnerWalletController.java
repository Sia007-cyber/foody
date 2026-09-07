package com.foody.wallet.controller;
import com.foody.auth.security.FoodyUserPrincipal; import com.foody.wallet.dto.*; import com.foody.wallet.service.WalletService; import jakarta.validation.Valid; import java.util.List;
import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/business/wallets") @PreAuthorize("hasRole('BUSINESS_OWNER')")
public class OwnerWalletController {private final WalletService service;public OwnerWalletController(WalletService s){service=s;}
 @GetMapping public List<WalletResponse> wallets(@AuthenticationPrincipal FoodyUserPrincipal p){return service.ownerWallets(p.getUserId());}
 @PostMapping("/customers/{customerId}/credit") public WalletResponse credit(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable Long customerId,@Valid @RequestBody AmountRequest r){return service.ownerCredit(p.getUserId(),customerId,r.amount());}
 @PostMapping("/customers/{customerId}/debit-requests") @ResponseStatus(HttpStatus.CREATED) public DebitRequestResponse debit(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable Long customerId,@Valid @RequestBody AmountRequest r){return service.ownerRequestDebit(p.getUserId(),customerId,r.amount());}}
