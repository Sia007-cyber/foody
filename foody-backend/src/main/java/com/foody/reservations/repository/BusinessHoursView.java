package com.foody.reservations.repository;

import java.time.LocalTime;

/** Read-only view of the existing business_hours configuration used by reservations. */
public interface BusinessHoursView {

    LocalTime getOpenTime();

    LocalTime getCloseTime();

    boolean getClosed();
}
