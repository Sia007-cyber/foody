package com.foody.reservations.service;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.reservations.dto.CreateReservationRequest;
import com.foody.reservations.dto.ReservationResponse;
import com.foody.reservations.entity.Reservation;
import com.foody.reservations.entity.ReservationStatus;
import com.foody.reservations.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReservationServiceImpl implements ReservationService {

    // Cancellable only from these states, per the Phase 1 state machine.
    private static final Set<ReservationStatus> CANCELLABLE_STATUSES =
            Set.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    private final ReservationRepository reservationRepository;
    private final BusinessService businessService;

    ReservationServiceImpl(ReservationRepository reservationRepository, BusinessService businessService) {
        this.reservationRepository = reservationRepository;
        this.businessService = businessService;
    }

    @Override
    @Transactional
    public ReservationResponse createReservation(Long customerUserId, CreateReservationRequest request) {
        Business business = businessService.findByIdAndStatus(request.businessId(), BusinessStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + request.businessId()));

        if (request.date().isBefore(LocalDate.now())) {
            throw new InvalidRequestException("Reservation date cannot be in the past");
        }

        Reservation reservation = new Reservation();
        reservation.setBusinessId(business.getId());
        reservation.setCustomerUserId(customerUserId);
        reservation.setReservationDate(request.date());
        reservation.setReservationTime(request.time());
        reservation.setGuestCount(request.guestCount());
        reservation.setStatus(ReservationStatus.PENDING);

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationForCustomer(Long reservationId, Long customerUserId) {
        return ReservationResponse.from(findOwnedReservation(reservationId, customerUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(Long customerUserId) {
        return reservationRepository
                .findByCustomerUserIdOrderByReservationDateDescReservationTimeDesc(customerUserId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public ReservationResponse cancelReservation(Long reservationId, Long customerUserId) {
        Reservation reservation = findOwnedReservation(reservationId, customerUserId);
        if (!CANCELLABLE_STATUSES.contains(reservation.getStatus())) {
            throw new InvalidStateTransitionException(
                    "Reservation " + reservationId + " can no longer be cancelled (status: "
                            + reservation.getStatus() + ")");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getAvailability(Long businessId, LocalDate date) {
        businessService.findByIdAndStatus(businessId, BusinessStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));

        return reservationRepository
                .findByBusinessIdAndReservationDateOrderByReservationTimeAsc(businessId, date).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    private Reservation findOwnedReservation(Long reservationId, Long customerUserId) {
        // Not-found-or-not-owned collapse to the same 404, same pattern as orders.
        return reservationRepository.findByIdAndCustomerUserId(reservationId, customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));
    }
}
