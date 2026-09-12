package com.foody.reservations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.AbstractContainerBaseTest;
import com.foody.auth.security.JwtService;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.reservations.entity.Reservation;
import com.foody.reservations.entity.ReservationStatus;
import com.foody.reservations.repository.ReservationRepository;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
// The base container restarts between classes; do not reuse a context with its old JDBC port.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
class ReservationPrivacyIntegrationTest extends AbstractContainerBaseTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired BusinessRepository businessRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    User customer;
    User otherCustomer;
    User owner;
    User otherOwner;
    Business business;
    Reservation reservation;
    LocalDate date;

    @BeforeEach
    void provisionReservation() {
        customer = createUser(UserRole.CUSTOMER);
        otherCustomer = createUser(UserRole.CUSTOMER);
        owner = createUser(UserRole.BUSINESS_OWNER);
        otherOwner = createUser(UserRole.BUSINESS_OWNER);
        business = createBusiness(owner);
        createBusiness(otherOwner);
        date = LocalDate.now().plusDays(1);
        reservation = new Reservation();
        reservation.setBusinessId(business.getId());
        reservation.setCustomerUserId(customer.getId());
        reservation.setReservationDate(date);
        reservation.setReservationTime(LocalTime.of(19, 0));
        reservation.setGuestCount(4);
        reservation = reservationRepository.saveAndFlush(reservation);
    }

    private User createUser(UserRole role) {
        User user = new User();
        String email = UUID.randomUUID() + "@foody.test";
        user.setEmail(email);
        user.setFullName("Private customer name");
        user.setPhone("09" + String.format("%09d", Math.floorMod(email.hashCode(), 1_000_000_000)));
        user.setPasswordHash("unused-in-jwt-fixture");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private Business createBusiness(User ownerUser) {
        Business result = new Business();
        result.setOwnerUserId(ownerUser.getId());
        result.setName("Privacy test cafe");
        result.setBusinessType("CAFE");
        result.setStatus(BusinessStatus.APPROVED);
        return businessRepository.saveAndFlush(result);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(user);
    }

    @Test
    void anonymousAvailability_containsOnlyPublicFieldsRegardlessOfBookings() throws Exception {
        String response = mockMvc.perform(get("/api/businesses/{id}/reservation-availability", business.getId())
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Exact shape rejects ANY extra record, identity, count, time, or metadata field.
        assertThat(objectMapper.readTree(response)).isEqualTo(objectMapper.valueToTree(Map.of(
                "date", date.toString(), "availabilityCalculated", false)));

        reservationRepository.delete(reservation);
        reservationRepository.flush();
        String emptyResponse = mockMvc.perform(get("/api/businesses/{id}/reservation-availability", business.getId())
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(emptyResponse)).isEqualTo(objectMapper.readTree(response));
    }

    @Test
    void customerCanCreateReservationWithinConfiguredBusinessHours() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO business_hours (business_id, day_of_week, open_time, close_time, is_closed)
                VALUES (?, ?, '08:00', '20:00', FALSE)
                """, business.getId(), date.getDayOfWeek().getValue());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "businessId", business.getId(),
                                "date", date.toString(),
                                "time", "18:00",
                                "guestCount", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessId").value(business.getId()))
                .andExpect(jsonPath("$.date").value(date.toString()))
                .andExpect(jsonPath("$.time").value("18:00:00"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void customer_retainsOwnDetailsAndCancellation() throws Exception {
        mockMvc.perform(get("/api/reservations/{id}", reservation.getId())
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservation.getId()))
                .andExpect(jsonPath("$.customerUserId").value(customer.getId()))
                .andExpect(jsonPath("$.businessId").value(business.getId()))
                .andExpect(jsonPath("$.date").value(date.toString()))
                .andExpect(jsonPath("$.time").value("19:00:00"))
                .andExpect(jsonPath("$.guestCount").value(4))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
        mockMvc.perform(get("/api/reservations/my").header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(reservation.getId()));
        mockMvc.perform(patch("/api/reservations/{id}/cancel", reservation.getId())
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void owner_retainsBusinessDetailsAndStatusManagement() throws Exception {
        mockMvc.perform(get("/api/business/reservations").param("date", date.toString())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(reservation.getId()))
                .andExpect(jsonPath("$[0].customerUserId").value(customer.getId()))
                .andExpect(jsonPath("$[0].date").value(date.toString()))
                .andExpect(jsonPath("$[0].time").value("19:00:00"))
                .andExpect(jsonPath("$[0].guestCount").value(4))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].createdAt").exists());
        mockMvc.perform(patch("/api/business/reservations/{id}/status", reservation.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservation.getId()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void unrelatedAccounts_cannotReadOrMutateReservation() throws Exception {
        for (User caller : new User[]{otherCustomer, otherOwner}) {
            mockMvc.perform(get("/api/reservations/{id}", reservation.getId())
                            .header("Authorization", bearer(caller)))
                    .andExpect(status().isNotFound());
            mockMvc.perform(patch("/api/reservations/{id}/cancel", reservation.getId())
                            .header("Authorization", bearer(caller)))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/api/reservations/my").header("Authorization", bearer(caller)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        }
        mockMvc.perform(get("/api/business/reservations").header("Authorization", bearer(otherOwner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(patch("/api/business/reservations/{id}/status", reservation.getId())
                        .header("Authorization", bearer(otherOwner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/business/reservations").header("Authorization", bearer(otherCustomer)))
                .andExpect(status().isForbidden());
        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void anonymousCaller_cannotReadDetailedEndpoints() throws Exception {
        mockMvc.perform(get("/api/reservations/{id}", reservation.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reservations/my")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/business/reservations")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = BusinessStatus.class, names = {"PENDING", "REJECTED", "SUSPENDED"})
    void anonymousAvailability_stillRequiresApprovedBusiness(BusinessStatus businessStatus) throws Exception {
        business.setStatus(businessStatus);
        businessRepository.saveAndFlush(business);
        mockMvc.perform(get("/api/businesses/{id}/reservation-availability", business.getId())
                        .param("date", date.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousAvailability_missingBusinessIsNotFound() throws Exception {
        mockMvc.perform(get("/api/businesses/{id}/reservation-availability", Long.MAX_VALUE)
                        .param("date", date.toString()))
                .andExpect(status().isNotFound());
    }
}
