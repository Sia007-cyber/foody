package com.foody.reservations.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.reservations.dto.CreateReservationRequest;
import com.foody.reservations.dto.ReservationResponse;
import com.foody.reservations.entity.ReservationStatus;
import com.foody.reservations.service.ReservationService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    static final Long CUSTOMER_ID = 1L;

    @Mock ReservationService reservationService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        ReservationController controller = new ReservationController(reservationService);

        User user = new User();
        user.setId(CUSTOMER_ID);
        user.setEmail("customer@foody.test");
        user.setFullName("Test Customer");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        FoodyUserPrincipal principal = new FoodyUserPrincipal(user);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(FoodyUserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return principal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    private ReservationResponse sampleReservation(ReservationStatus status) {
        return new ReservationResponse(
                100L, 10L, CUSTOMER_ID, LocalDate.now().plusDays(1), LocalTime.of(19, 0), 4,
                status, Instant.now(), Instant.now());
    }

    @Test
    void createReservation_returnsCreatedReservation() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest(
                10L, LocalDate.now().plusDays(1), LocalTime.of(19, 0), 4);

        when(reservationService.createReservation(eq(CUSTOMER_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(sampleReservation(ReservationStatus.PENDING));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.guestCount").value(4));
    }

    @Test
    void createReservation_rejectsMissingGuestCount() throws Exception {
        String body = "{\"businessId\":10,\"date\":\"" + LocalDate.now().plusDays(1) + "\",\"time\":\"19:00:00\"}";

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void myReservations_returnsCustomersReservations() throws Exception {
        when(reservationService.getMyReservations(CUSTOMER_ID))
                .thenReturn(List.of(sampleReservation(ReservationStatus.PENDING)));

        mockMvc.perform(get("/api/reservations/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    void getAvailability_returnsReservationsForDate() throws Exception {
        when(reservationService.getAvailability(eq(10L), eq(LocalDate.now())))
                .thenReturn(List.of(sampleReservation(ReservationStatus.CONFIRMED)));

        mockMvc.perform(get("/api/businesses/10/reservation-availability")
                        .param("date", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void cancel_returnsUpdatedReservation() throws Exception {
        when(reservationService.cancelReservation(100L, CUSTOMER_ID))
                .thenReturn(sampleReservation(ReservationStatus.CANCELLED));

        mockMvc.perform(patch("/api/reservations/100/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_returnsConflictWhenNotCancellable() throws Exception {
        when(reservationService.cancelReservation(100L, CUSTOMER_ID))
                .thenThrow(new InvalidStateTransitionException("Reservation 100 can no longer be cancelled"));

        mockMvc.perform(patch("/api/reservations/100/cancel"))
                .andExpect(status().isConflict());
    }
}
