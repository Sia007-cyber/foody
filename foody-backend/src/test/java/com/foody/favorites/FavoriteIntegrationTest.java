package com.foody.favorites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.AbstractContainerBaseTest;
import com.foody.auth.security.JwtService;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.favorites.service.FavoriteService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class FavoriteIntegrationTest extends AbstractContainerBaseTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BusinessRepository businesses;
    @Autowired FavoriteService favorites;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;

    @Test
    void customerCanAddListAndRemoveOnlyOwnFavorite() throws Exception {
        User first = user(UserRole.CUSTOMER);
        User second = user(UserRole.CUSTOMER);
        Business business = business(BusinessStatus.APPROVED);

        mvc.perform(put("/api/favorites/{id}", business.getId()).header("Authorization", bearer(first)))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/favorites/{id}", business.getId()).header("Authorization", bearer(first)))
                .andExpect(status().isNoContent());
        assertThat(count(first.getId(), business.getId())).isEqualTo(1);

        mvc.perform(get("/api/favorites").header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(business.getId()));
        mvc.perform(get("/api/favorites/business-ids").header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value(business.getId()));
        mvc.perform(get("/api/favorites").header("Authorization", bearer(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        mvc.perform(delete("/api/favorites/{id}", business.getId()).header("Authorization", bearer(second)))
                .andExpect(status().isNoContent());
        assertThat(count(first.getId(), business.getId())).isEqualTo(1);
        mvc.perform(delete("/api/favorites/{id}", business.getId()).header("Authorization", bearer(first)))
                .andExpect(status().isNoContent());
        assertThat(count(first.getId(), business.getId())).isZero();
    }

    @Test
    void concurrentDuplicateAddsAreIdempotent() throws Exception {
        User customer = user(UserRole.CUSTOMER);
        Business business = business(BusinessStatus.APPROVED);
        var executor = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Void>> calls = new ArrayList<>();
            for (int i = 0; i < 12; i++) calls.add(() -> { favorites.add(customer.getId(), business.getId()); return null; });
            for (var result : executor.invokeAll(calls)) result.get();
        } finally {
            executor.shutdownNow();
        }
        assertThat(count(customer.getId(), business.getId())).isEqualTo(1);
    }

    @Test
    void favoriteEndpointsEnforceCustomerAuthentication() throws Exception {
        Business business = business(BusinessStatus.APPROVED);
        mvc.perform(put("/api/favorites/{id}", business.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/favorites").header("Authorization", bearer(user(UserRole.BUSINESS_OWNER))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/favorites").header("Authorization", bearer(user(UserRole.ADMIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonPublicAndMissingBusinessesCannotBeFavoritedOrExposed() throws Exception {
        User customer = user(UserRole.CUSTOMER);
        Business approved = business(BusinessStatus.APPROVED);
        Business pending = business(BusinessStatus.PENDING);
        String auth = bearer(customer);

        mvc.perform(put("/api/favorites/{id}", pending.getId()).header("Authorization", auth))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/favorites/{id}", Long.MAX_VALUE).header("Authorization", auth))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/favorites/{id}", approved.getId()).header("Authorization", auth))
                .andExpect(status().isNoContent());
        approved.setStatus(BusinessStatus.SUSPENDED);
        businesses.saveAndFlush(approved);

        mvc.perform(get("/api/favorites").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/favorites/business-ids").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        assertThat(count(customer.getId(), approved.getId())).isEqualTo(1);
    }

    private long count(Long customerId, Long businessId) {
        return jdbc.queryForObject("select count(*) from business_favorites where customer_user_id=? and business_id=?",
                Long.class, customerId, businessId);
    }

    private User user(UserRole role) {
        User user = new User(); user.setEmail(UUID.randomUUID() + "@favorites.test"); user.setFullName("Favorite test");
        user.setPasswordHash("unused"); user.setRole(role); user.setStatus(UserStatus.ACTIVE); return users.saveAndFlush(user);
    }

    private Business business(BusinessStatus status) {
        Business business = new Business(); business.setOwnerUserId(user(UserRole.BUSINESS_OWNER).getId());
        business.setName("Favorite cafe " + UUID.randomUUID()); business.setBusinessType("CAFE");
        business.setCity("تهران"); business.setStatus(status); return businesses.saveAndFlush(business);
    }

    private String bearer(User user) { return "Bearer " + jwt.generateAccessToken(user); }
}
