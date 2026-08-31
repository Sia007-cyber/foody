package com.foody.reservations.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateReservationRequest(

        @NotNull(message = "businessId is required")
        Long businessId,

        @NotNull(message = "date is required")
        LocalDate date,

        @NotNull(message = "time is required")
        LocalTime time,

        @NotNull(message = "guestCount is required")
        @Min(value = 1, message = "guestCount must be at least 1")
        Integer guestCount
) {
}
