package com.foody.reservations.service;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.notifications.entity.NotificationType;
import com.foody.notifications.service.NotificationService;
import com.foody.reservations.dto.CreateReservationRequest;
import com.foody.reservations.dto.ReservationResponse;
import com.foody.reservations.entity.Reservation;
import com.foody.reservations.entity.ReservationStatus;
import com.foody.reservations.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReservationServiceImpl implements ReservationService {

    // Cancellable only from these states, per the Phase 1 state machine.
    private static final Set<ReservationStatus> CANCELLABLE_STATUSES =
            Set.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    // Business-owner-driven transitions, per the Phase 1 state machine:
    // PENDING -> CONFIRMED -> COMPLETED
    //         -> REJECTED
    // (CANCELLED is customer-only, handled separately in cancelReservation.)
    private static final Map<ReservationStatus, Set<ReservationStatus>> VALID_BUSINESS_TRANSITIONS = Map.of(
            ReservationStatus.PENDING, Set.of(ReservationStatus.CONFIRMED, ReservationStatus.REJECTED),
            ReservationStatus.CONFIRMED, Set.of(ReservationStatus.COMPLETED)
    );

    private final ReservationRepository reservationRepository;
    private final BusinessService businessService;
    private final NotificationService notificationService;

    ReservationServiceImpl(ReservationRepository reservationRepository, BusinessService businessService,
                           NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.businessService = businessService;
        this.notificationService = notificationService;
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

        Reservation saved = reservationRepository.save(reservation);

        notificationService.notify(business.getOwnerUserId(), NotificationType.NEW_RESERVATION,
                "رزرو جدید",
                "یک رزرو جدید به شماره #" + saved.getId() + " برای کسب‌وکار شما ثبت شد.",
                "RESERVATION", saved.getId());

        return ReservationResponse.from(saved);
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
        Reservation saved = reservationRepository.save(reservation);

        businessService.findById(saved.getBusinessId()).ifPresent(business ->
                notificationService.notify(business.getOwnerUserId(), NotificationType.RESERVATION_STATUS_CHANGED,
                        "لغو رزرو",
                        "رزرو #" + saved.getId() + " توسط مشتری لغو شد.",
                        "RESERVATION", saved.getId()));

        return ReservationResponse.from(saved);
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

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getBusinessReservations(Long ownerUserId, LocalDate dateFilter) {
        Business business = requireOwnedBusiness(ownerUserId);
        List<Reservation> reservations = dateFilter != null
                ? reservationRepository.findByBusinessIdAndReservationDateOrderByReservationTimeAsc(
                        business.getId(), dateFilter)
                : reservationRepository.findByBusinessIdOrderByReservationDateDescReservationTimeDesc(
                        business.getId());
        return reservations.stream().map(ReservationResponse::from).toList();
    }

    @Override
    @Transactional
    public ReservationResponse updateReservationStatus(Long ownerUserId, Long reservationId,
                                                        ReservationStatus newStatus) {
        Business business = requireOwnedBusiness(ownerUserId);
        Reservation reservation = reservationRepository.findByIdAndBusinessId(reservationId, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        Set<ReservationStatus> allowedNext =
                VALID_BUSINESS_TRANSITIONS.getOrDefault(reservation.getStatus(), Set.of());
        if (!allowedNext.contains(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Cannot move reservation " + reservationId + " from " + reservation.getStatus()
                            + " to " + newStatus);
        }
        reservation.setStatus(newStatus);
        Reservation saved = reservationRepository.save(reservation);

        notificationService.notify(saved.getCustomerUserId(), NotificationType.RESERVATION_STATUS_CHANGED,
                "به‌روزرسانی رزرو",
                "وضعیت رزرو #" + saved.getId() + " به " + statusLabel(newStatus) + " تغییر کرد.",
                "RESERVATION", saved.getId());

        return ReservationResponse.from(saved);
    }

    private String statusLabel(ReservationStatus status) {
        return switch (status) {
            case PENDING -> "در انتظار تایید";
            case CONFIRMED -> "تایید شده";
            case COMPLETED -> "تکمیل شده";
            case REJECTED -> "رد شده";
            case CANCELLED -> "لغو شده";
        };
    }

    private Business requireOwnedBusiness(Long ownerUserId) {
        return businessService.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No business found for this owner"));
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return reservationRepository.count();
    }
}
