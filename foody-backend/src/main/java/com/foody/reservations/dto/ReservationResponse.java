package com.foody.reservations.dto;

import com.foody.reservations.entity.Reservation;
import com.foody.reservations.entity.ReservationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** Public view of a reservation. Returned instead of the entity. */
public record ReservationResponse(
        Long id,
        Long businessId,
        Long customerUserId,
        LocalDate date,
        LocalTime time,
        Integer guestCount,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(), r.getBusinessId(), r.getCustomerUserId(),
                r.getReservationDate(), r.getReservationTime(), r.getGuestCount(),
                r.getStatus(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
