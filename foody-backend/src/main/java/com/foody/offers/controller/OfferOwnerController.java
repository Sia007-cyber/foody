package com.foody.offers.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.offers.dto.*;
import com.foody.offers.service.OfferService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/business/offers") @PreAuthorize("hasRole('BUSINESS_OWNER')")
public class OfferOwnerController {
    private final OfferService service; public OfferOwnerController(OfferService service){this.service=service;}
    @GetMapping public List<OfferResponse> list(@AuthenticationPrincipal FoodyUserPrincipal p){return service.ownerOffers(p.getUserId());}
    @PostMapping public OfferResponse create(@AuthenticationPrincipal FoodyUserPrincipal p,@Valid @RequestBody CreateOfferRequest r){return service.create(p.getUserId(),r);}
    @PatchMapping("/{id}/cancel") public OfferResponse cancel(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable Long id){return service.cancel(p.getUserId(),id);}
}
