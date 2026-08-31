package com.foody.orders.entity;

/**
 * Order lifecycle, per the Phase 1 spec:
 * PENDING -> ACCEPTED -> PREPARING -> READY -> COMPLETED
 *         -> REJECTED
 * PENDING -> CANCELLED (by customer, only while still PENDING)
 */
public enum OrderStatus {
    PENDING,
    ACCEPTED,
    PREPARING,
    READY,
    COMPLETED,
    REJECTED,
    CANCELLED
}
