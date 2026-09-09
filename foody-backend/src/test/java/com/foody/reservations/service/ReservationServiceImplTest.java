package com.foody.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.common.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.foody.notifications.service.NotificationService;
import com.foody.reservations.dto.CreateReservationRequest;
import com.foody.reservations.dto.ReservationAvailabilityResponse;
import com.foody.reservations.dto.ReservationResponse;
import com.foody.reservations.entity.Reservation;
import com.foody.reservations.entity.ReservationStatus;
import com.foody.reservations.repository.BusinessHoursRepository;
import com.foody.reservations.repository.BusinessHoursView;
import com.foody.reservations.repository.ReservationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock ReservationRepository reservationRepository;
    @Mock BusinessHoursRepository businessHoursRepository;
    @Mock BusinessService businessService;
    @Mock NotificationService notificationService;

    ReservationServiceImpl reservationService;

    static final Long CUSTOMER_ID = 1L;
    static final Long BUSINESS_ID = 10L;
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Tehran");
    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-09T08:30:00Z"), BUSINESS_ZONE);
    static final LocalDate TODAY = LocalDate.of(2026, 9, 9);

    @BeforeEach
    void setUp() {
        reservationService = new ReservationServiceImpl(
                reservationRepository, businessHoursRepository, businessService, notificationService, FIXED_CLOCK);
    }

    private Business approvedBusiness() {
        Business business = new Business();
        business.setId(BUSINESS_ID);
        business.setStatus(BusinessStatus.APPROVED);
        return business;
    }

    @Test
    void createReservation_savesWithPendingStatus() {
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness()));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateReservationRequest request = new CreateReservationRequest(
                BUSINESS_ID, TODAY.plusDays(1), LocalTime.of(19, 0), 4);

        ReservationResponse response = reservationService.createReservation(CUSTOMER_ID, request);

        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(response.guestCount()).isEqualTo(4);
        assertThat(response.businessId()).isEqualTo(BUSINESS_ID);
    }

    @Test
    void businessOwnerCannotReserveOwnBusiness() {
        Business business = approvedBusiness();
        business.setOwnerUserId(CUSTOMER_ID);
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(business));
        assertThatThrownBy(() -> reservationService.createReservation(CUSTOMER_ID,
                new CreateReservationRequest(BUSINESS_ID, TODAY.plusDays(1), LocalTime.of(19, 0), 2)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createReservation_rejectsPastDate() {
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness()));

        CreateReservationRequest request = new CreateReservationRequest(
                BUSINESS_ID, TODAY.minusDays(1), LocalTime.of(19, 0), 2);

        assertThatThrownBy(() -> reservationService.createReservation(CUSTOMER_ID, request))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void createReservation_rejectsSameDayPastTime() {
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness()));

        CreateReservationRequest request = new CreateReservationRequest(
                BUSINESS_ID, TODAY, LocalTime.of(11, 59), 2);

        assertThatThrownBy(() -> reservationService.createReservation(CUSTOMER_ID, request))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void createReservation_acceptsSameDayFutureTimeWhenOtherwiseValid() {
        stubSuccessfulCreation();
        when(businessHoursRepository.findForDay(BUSINESS_ID, TODAY.getDayOfWeek().getValue()))
                .thenReturn(List.of(hours(LocalTime.of(8, 0), LocalTime.of(20, 0), false)));

        ReservationResponse response = reservationService.createReservation(CUSTOMER_ID,
                new CreateReservationRequest(BUSINESS_ID, TODAY, LocalTime.of(13, 0), 2));

        assertThat(response.date()).isEqualTo(TODAY);
        assertThat(response.time()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    void createReservation_acceptsFutureDateWhenOtherwiseValid() {
        LocalDate futureDate = TODAY.plusDays(2);
        stubSuccessfulCreation();
        when(businessHoursRepository.findForDay(BUSINESS_ID, futureDate.getDayOfWeek().getValue()))
                .thenReturn(List.of(hours(LocalTime.of(8, 0), LocalTime.of(20, 0), false)));

        ReservationResponse response = reservationService.createReservation(CUSTOMER_ID,
                new CreateReservationRequest(BUSINESS_ID, futureDate, LocalTime.of(19, 0), 2));

        assertThat(response.date()).isEqualTo(futureDate);
    }

    @Test
    void createReservation_rejectsClosedBusinessDay() {
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness()));
        LocalDate futureDate = TODAY.plusDays(1);
        when(businessHoursRepository.findForDay(BUSINESS_ID, futureDate.getDayOfWeek().getValue()))
                .thenReturn(List.of(hours(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, true)));

        assertThatThrownBy(() -> reservationService.createReservation(CUSTOMER_ID,
                new CreateReservationRequest(BUSINESS_ID, futureDate, LocalTime.NOON, 2)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void createReservation_rejectsOutsideBusinessHours() {
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness()));
        LocalDate futureDate = TODAY.plusDays(1);
        when(businessHoursRepository.findForDay(BUSINESS_ID, futureDate.getDayOfWeek().getValue()))
                .thenReturn(List.of(hours(LocalTime.of(8, 0), LocalTime.of(20, 0), false)));

        assertThatThrownBy(() -> reservationService.createReservation(CUSTOMER_ID,
                new CreateReservationRequest(BUSINESS_ID, futureDate, LocalTime.of(20, 0), 2)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void createReservation_succeedsWithinBusinessHours() {
        LocalDate futureDate = TODAY.plusDays(1);
        stubSuccessfulCreation();
        when(businessHoursRepository.findForDay(BUSINESS_ID, futureDate.getDayOfWeek().getValue()))
                .thenReturn(List.of(hours(LocalTime.of(8, 0), LocalTime.of(20, 0), false)));

        ReservationResponse response = reservationService.createReservation(CUSTOMER_ID,
                new CreateReservationRequest(BUSINESS_ID, futureDate, LocalTime.of(19, 0), 2));

        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
    }

    private void stubSuccessfulCreation() {
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness()));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private BusinessHoursView hours(LocalTime open, LocalTime close, boolean closed) {
        return new BusinessHoursView() {
            public LocalTime getOpenTime() { return open; }
            public LocalTime getCloseTime() { return close; }
            public boolean getClosed() { return closed; }
        };
    }

    @Test
    void createReservation_rejectsWhenBusinessNotApproved() {
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.empty());

        CreateReservationRequest request = new CreateReservationRequest(
                BUSINESS_ID, TODAY.plusDays(1), LocalTime.of(19, 0), 2);

        assertThatThrownBy(() -> reservationService.createReservation(CUSTOMER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelReservation_succeedsWhenPending() {
        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setBusinessId(BUSINESS_ID);
        reservation.setCustomerUserId(CUSTOMER_ID);
        reservation.setReservationDate(LocalDate.now().plusDays(1));
        reservation.setReservationTime(LocalTime.of(19, 0));
        reservation.setGuestCount(2);
        reservation.setStatus(ReservationStatus.PENDING);

        when(reservationRepository.findByIdAndCustomerUserId(100L, CUSTOMER_ID))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.cancelReservation(100L, CUSTOMER_ID);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void cancelReservation_succeedsWhenConfirmed() {
        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setBusinessId(BUSINESS_ID);
        reservation.setCustomerUserId(CUSTOMER_ID);
        reservation.setReservationDate(LocalDate.now().plusDays(1));
        reservation.setReservationTime(LocalTime.of(19, 0));
        reservation.setGuestCount(2);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findByIdAndCustomerUserId(100L, CUSTOMER_ID))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.cancelReservation(100L, CUSTOMER_ID);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void cancelReservation_rejectsWhenAlreadyCompleted() {
        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setBusinessId(BUSINESS_ID);
        reservation.setCustomerUserId(CUSTOMER_ID);
        reservation.setReservationDate(LocalDate.now());
        reservation.setReservationTime(LocalTime.of(19, 0));
        reservation.setGuestCount(2);
        reservation.setStatus(ReservationStatus.COMPLETED);

        when(reservationRepository.findByIdAndCustomerUserId(100L, CUSTOMER_ID))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(100L, CUSTOMER_ID))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void getAvailability_reportsUncalculatedWithoutReadingReservations() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness()));

        ReservationAvailabilityResponse result = reservationService.getAvailability(BUSINESS_ID, date);

        assertThat(result.date()).isEqualTo(date);
        assertThat(result.availabilityCalculated()).isFalse();
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void getReservationForCustomer_throwsWhenNotOwned() {
        when(reservationRepository.findByIdAndCustomerUserId(100L, CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.getReservationForCustomer(100L, CUSTOMER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateReservationStatus_allowsPendingToConfirmed() {
        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setBusinessId(BUSINESS_ID);
        reservation.setCustomerUserId(CUSTOMER_ID);
        reservation.setReservationDate(LocalDate.now().plusDays(1));
        reservation.setReservationTime(LocalTime.of(19, 0));
        reservation.setGuestCount(2);
        reservation.setStatus(ReservationStatus.PENDING);

        when(businessService.findByOwnerUserId(5L)).thenReturn(Optional.of(approvedBusiness()));
        when(reservationRepository.findByIdAndBusinessId(100L, BUSINESS_ID)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.updateReservationStatus(
                5L, 100L, ReservationStatus.CONFIRMED);

        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void updateReservationStatus_rejectsCompletingWithoutConfirmation() {
        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setBusinessId(BUSINESS_ID);
        reservation.setCustomerUserId(CUSTOMER_ID);
        reservation.setReservationDate(LocalDate.now().plusDays(1));
        reservation.setReservationTime(LocalTime.of(19, 0));
        reservation.setGuestCount(2);
        reservation.setStatus(ReservationStatus.PENDING);

        when(businessService.findByOwnerUserId(5L)).thenReturn(Optional.of(approvedBusiness()));
        when(reservationRepository.findByIdAndBusinessId(100L, BUSINESS_ID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() ->
                reservationService.updateReservationStatus(5L, 100L, ReservationStatus.COMPLETED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
