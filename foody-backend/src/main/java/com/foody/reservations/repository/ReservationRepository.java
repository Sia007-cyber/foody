package com.foody.reservations.repository;

import com.foody.reservations.entity.Reservation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByIdAndCustomerUserId(Long id, Long customerUserId);

    List<Reservation> findByCustomerUserIdOrderByReservationDateDescReservationTimeDesc(Long customerUserId);

    // Phase 1: simple availability = all reservations for this business on this date,
    // no capacity limit (see decision log). Used by the customer app to show what's
    // already booked; a real capacity check can replace this later without an API change.
    // Also reused by the business panel's date-filtered reservation list.
    List<Reservation> findByBusinessIdAndReservationDateOrderByReservationTimeAsc(
            Long businessId, LocalDate reservationDate);

    Optional<Reservation> findByIdAndBusinessId(Long id, Long businessId);

    List<Reservation> findByBusinessIdOrderByReservationDateDescReservationTimeDesc(Long businessId);
}
