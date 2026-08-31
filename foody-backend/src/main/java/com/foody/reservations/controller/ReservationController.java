package com.foody.reservations.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.reservations.dto.CreateReservationRequest;
import com.foody.reservations.dto.ReservationResponse;
import com.foody.reservations.service.ReservationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-facing reservation endpoints. Availability is public (mirrors business/menu
 * browsing); creating, viewing own reservations, and cancelling require authentication.
 * Business-owner endpoints for confirming/rejecting reservations arrive with the
 * business panel (see WebSecurityConfig for the public matcher covering availability).
 */
@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/businesses/{businessId}/reservation-availability")
    public List<ReservationResponse> getAvailability(
            @PathVariable Long businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.getAvailability(businessId, date);
    }

    @PostMapping("/reservations")
    public ReservationResponse createReservation(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                                  @Valid @RequestBody CreateReservationRequest request) {
        return reservationService.createReservation(principal.getUserId(), request);
    }

    @GetMapping("/reservations/my")
    public List<ReservationResponse> myReservations(@AuthenticationPrincipal FoodyUserPrincipal principal) {
        return reservationService.getMyReservations(principal.getUserId());
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse getById(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                       @PathVariable Long id) {
        return reservationService.getReservationForCustomer(id, principal.getUserId());
    }

    @PatchMapping("/reservations/{id}/cancel")
    public ReservationResponse cancel(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                      @PathVariable Long id) {
        return reservationService.cancelReservation(id, principal.getUserId());
    }
}
