package com.foody.reservations.dto;

import java.time.LocalDate;

/**
 * Anonymous availability contract, independent of individual reservation records.
 * The date echoes the requested date. availabilityCalculated is currently always
 * false: there is no capacity, table allocation, slot calculation, or business-hours
 * validation. False means availability is unknown, not that the date is unavailable.
 * No booking times, counts, identities, or record metadata are exposed.
 */
public record ReservationAvailabilityResponse(LocalDate date, boolean availabilityCalculated) {
}
