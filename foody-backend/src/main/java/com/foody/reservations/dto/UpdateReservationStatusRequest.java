package com.foody.reservations.dto;

import com.foody.reservations.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReservationStatusRequest(

        @NotNull(message = "status is required")
        ReservationStatus status
) {
}
