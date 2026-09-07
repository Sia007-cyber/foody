package com.foody.offers.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.offers.dto.*;
import com.foody.offers.service.OfferService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/offers")
public class OfferController {
    private final OfferService service; public OfferController(OfferService service){this.service=service;}
    @GetMapping public List<OfferResponse> list(){return service.claimableOffers();}
    @PostMapping("/{id}/claim") @PreAuthorize("hasRole('CUSTOMER')")
    public OfferClaimResponse claim(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable Long id){return service.claim(p.getUserId(),id);}
    @GetMapping("/my-claims") @PreAuthorize("hasRole('CUSTOMER')")
    public List<OfferClaimResponse> myClaims(@AuthenticationPrincipal FoodyUserPrincipal p){return service.myClaims(p.getUserId());}
}
