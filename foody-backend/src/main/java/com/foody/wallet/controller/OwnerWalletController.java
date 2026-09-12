package com.foody.wallet.controller;
import com.foody.auth.security.FoodyUserPrincipal; import com.foody.wallet.dto.*; import com.foody.wallet.service.WalletService; import jakarta.validation.Valid; import java.util.List;
import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/business/wallets") @PreAuthorize("hasRole('BUSINESS_OWNER')")
public class OwnerWalletController {private final WalletService service;public OwnerWalletController(WalletService s){service=s;}
 @GetMapping public List<OwnerWalletResponse> wallets(@AuthenticationPrincipal FoodyUserPrincipal p){return service.ownerWallets(p.getUserId());}
 @GetMapping("/customers/{publicId}") public CustomerLookupResponse lookup(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable String publicId){return service.ownerLookupCustomer(p.getUserId(),publicId);}
 @PostMapping("/customers/{publicId}/credit") public OwnerWalletResponse credit(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable String publicId,@Valid @RequestBody AmountRequest r){return service.ownerCreditByPublicId(p.getUserId(),publicId,r.amount());}
 @PostMapping("/customers/{publicId}/debit-requests") @ResponseStatus(HttpStatus.CREATED) public DebitRequestResponse debit(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable String publicId,@Valid @RequestBody AmountRequest r){return service.ownerRequestDebitByPublicId(p.getUserId(),publicId,r.amount());}
 @PostMapping("/customers/{publicId}/purchases") @ResponseStatus(HttpStatus.CREATED) public DebitRequestResponse purchase(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable String publicId,@Valid @RequestBody CreatePurchaseRequest r){return service.ownerCreatePurchase(p.getUserId(),publicId,r);}
 @GetMapping("/sales-history") public List<PurchaseHistoryResponse> sales(@AuthenticationPrincipal FoodyUserPrincipal p){return service.ownerSalesHistory(p.getUserId());}
}
