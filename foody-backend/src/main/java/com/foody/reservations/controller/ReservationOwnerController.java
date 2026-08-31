package com.foody.reservations.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.reservations.dto.ReservationResponse;
import com.foody.reservations.dto.UpdateReservationStatusRequest;
import com.foody.reservations.service.ReservationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Business panel — reservations at the calling owner's own business. */
@RestController
@RequestMapping("/api/business/reservations")
@PreAuthorize("hasRole('BUSINESS_OWNER')")
public class ReservationOwnerController {

    private final ReservationService reservationService;

    public ReservationOwnerController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public List<ReservationResponse> getBusinessReservations(
            @AuthenticationPrincipal FoodyUserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.getBusinessReservations(principal.getUserId(), date);
    }

    @PatchMapping("/{id}/status")
    public ReservationResponse updateStatus(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                            @PathVariable Long id,
                                            @Valid @RequestBody UpdateReservationStatusRequest request) {
        return reservationService.updateReservationStatus(principal.getUserId(), id, request.status());
    }
}
