package com.foody.favorites.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.businesses.dto.BusinessResponse;
import com.foody.favorites.service.FavoriteService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("hasRole('CUSTOMER')")
public class FavoriteController {
    private final FavoriteService favorites;
    public FavoriteController(FavoriteService favorites) { this.favorites = favorites; }
    @PutMapping("/{businessId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@AuthenticationPrincipal FoodyUserPrincipal principal, @PathVariable Long businessId) { favorites.add(principal.getUserId(), businessId); }
    @DeleteMapping("/{businessId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal FoodyUserPrincipal principal, @PathVariable Long businessId) { favorites.remove(principal.getUserId(), businessId); }
    @GetMapping public List<BusinessResponse> list(@AuthenticationPrincipal FoodyUserPrincipal principal) { return favorites.listPublicBusinesses(principal.getUserId()).stream().map(BusinessResponse::from).toList(); }
    @GetMapping("/business-ids") public List<Long> ids(@AuthenticationPrincipal FoodyUserPrincipal principal) { return favorites.listBusinessIds(principal.getUserId()); }
}
