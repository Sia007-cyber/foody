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

    // Detailed date-filtered list for the authenticated owning business only.
    List<Reservation> findByBusinessIdAndReservationDateOrderByReservationTimeAsc(
            Long businessId, LocalDate reservationDate);

    Optional<Reservation> findByIdAndBusinessId(Long id, Long businessId);

    List<Reservation> findByBusinessIdOrderByReservationDateDescReservationTimeDesc(Long businessId);
}
