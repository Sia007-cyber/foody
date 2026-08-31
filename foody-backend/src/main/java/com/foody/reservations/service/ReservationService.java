package com.foody.reservations.service;

import com.foody.reservations.dto.CreateReservationRequest;
import com.foody.reservations.dto.ReservationResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * Public contract for the reservations module. Other modules depend on this interface only.
 */
public interface ReservationService {

    ReservationResponse createReservation(Long customerUserId, CreateReservationRequest request);

    ReservationResponse getReservationForCustomer(Long reservationId, Long customerUserId);

    List<ReservationResponse> getMyReservations(Long customerUserId);

    ReservationResponse cancelReservation(Long reservationId, Long customerUserId);

    // Phase 1 "availability": existing reservations for a business on a given date,
    // with no capacity limit — see decision log.
    List<ReservationResponse> getAvailability(Long businessId, LocalDate date);
}
