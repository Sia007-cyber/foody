package com.foody.reservations.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Reservation-only read access to the business_hours table. */
public interface BusinessHoursRepository extends Repository<com.foody.businesses.entity.Business, Long> {

    @Query(value = """
            SELECT open_time AS openTime, close_time AS closeTime, is_closed AS closed
            FROM business_hours
            WHERE business_id = :businessId AND day_of_week = :dayOfWeek
            """, nativeQuery = true)
    List<BusinessHoursView> findForDay(@Param("businessId") Long businessId,
                                       @Param("dayOfWeek") int dayOfWeek);
}
