package com.foody.reservations.entity;

/**
 * Reservation lifecycle, per the Phase 1 spec:
 * PENDING -> CONFIRMED -> COMPLETED
 *         -> REJECTED
 * PENDING/CONFIRMED -> CANCELLED (by customer)
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    REJECTED,
    CANCELLED
}
