package com.foody.reservations.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.reservations.dto.ReservationResponse;
import com.foody.reservations.dto.UpdateReservationStatusRequest;
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

/**
 * Note: standalone MockMvc doesn't process @PreAuthorize (no method-security
 * interceptor wired), so these tests exercise request/response mapping only.
 * The real authorization guard is the ownership check inside the service layer.
 */
@ExtendWith(MockitoExtension.class)
class ReservationOwnerControllerTest {

    static final Long OWNER_ID = 1L;

    @Mock ReservationService reservationService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        ReservationOwnerController controller = new ReservationOwnerController(reservationService);

        User user = new User();
        user.setId(OWNER_ID);
        user.setEmail("owner@foody.test");
        user.setFullName("Test Owner");
        user.setRole(UserRole.BUSINESS_OWNER);
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
                200L, 10L, 5L, LocalDate.of(2026, 9, 10), LocalTime.of(19, 30), 4,
                status, Instant.now(), Instant.now());
    }

    @Test
    void getBusinessReservations_returnsOwnersReservations() throws Exception {
        when(reservationService.getBusinessReservations(OWNER_ID, null))
                .thenReturn(List.of(sampleReservation(ReservationStatus.PENDING)));

        mockMvc.perform(get("/api/business/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(200))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getBusinessReservations_filtersByDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(reservationService.getBusinessReservations(OWNER_ID, date))
                .thenReturn(List.of(sampleReservation(ReservationStatus.CONFIRMED)));

        mockMvc.perform(get("/api/business/reservations").param("date", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void updateStatus_returnsUpdatedReservation() throws Exception {
        when(reservationService.updateReservationStatus(eq(OWNER_ID), eq(200L), eq(ReservationStatus.CONFIRMED)))
                .thenReturn(sampleReservation(ReservationStatus.CONFIRMED));

        UpdateReservationStatusRequest request = new UpdateReservationStatusRequest(ReservationStatus.CONFIRMED);

        mockMvc.perform(patch("/api/business/reservations/200/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateStatus_rejectsMissingStatus() throws Exception {
        String body = "{}";

        mockMvc.perform(patch("/api/business/reservations/200/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_returnsConflictOnInvalidTransition() throws Exception {
        when(reservationService.updateReservationStatus(eq(OWNER_ID), eq(200L), eq(ReservationStatus.COMPLETED)))
                .thenThrow(new InvalidStateTransitionException(
                        "Reservation 200 cannot move to COMPLETED from PENDING"));

        UpdateReservationStatusRequest request = new UpdateReservationStatusRequest(ReservationStatus.COMPLETED);

        mockMvc.perform(patch("/api/business/reservations/200/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStatus_returns404WhenReservationNotOwnedByCaller() throws Exception {
        when(reservationService.updateReservationStatus(eq(OWNER_ID), eq(999L), eq(ReservationStatus.CONFIRMED)))
                .thenThrow(new ResourceNotFoundException("Reservation not found for owner"));

        UpdateReservationStatusRequest request = new UpdateReservationStatusRequest(ReservationStatus.CONFIRMED);

        mockMvc.perform(patch("/api/business/reservations/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
