package com.foody.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.AbstractContainerBaseTest;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class DemoAccountBootstrapIntegrationTest extends AbstractContainerBaseTest {
    @Autowired DemoAccountBootstrapService bootstrap;
    @Autowired DemoAccountsProperties properties;
    @Autowired UserRepository users;
    @Autowired BusinessRepository businesses;
    @Autowired PasswordEncoder passwords;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    String ownerPassword;
    String adminPassword;

    @BeforeEach
    void disabledBaseline() {
        properties.setEnabled(false);
        properties.setOwnerPassword(null);
        properties.setAdminPassword(null);
        bootstrap.apply();
        ownerPassword = "test-" + UUID.randomUUID();
        adminPassword = "test-" + UUID.randomUUID();
    }

    @Test
    void disabledModeKeepsV19OwnerAndAdminUnauthenticated() throws Exception {
        login(DemoAccountBootstrapService.OWNER_EMAIL, ownerPassword).andExpect(status().isUnauthorized());
        login(DemoAccountBootstrapService.ADMIN_EMAIL, adminPassword).andExpect(status().isUnauthorized());
        assertThat(users.findById(DemoAccountBootstrapService.OWNER_ID).orElseThrow().getStatus())
                .isEqualTo(UserStatus.SUSPENDED);
        assertThat(users.findById(DemoAccountBootstrapService.ADMIN_ID).orElseThrow().getStatus())
                .isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    void enabledModeRestoresExpectedRolesAndAuthenticationOnly() throws Exception {
        enable();
        bootstrap.apply();
        login(DemoAccountBootstrapService.OWNER_EMAIL, ownerPassword).andExpect(status().isOk());
        login(DemoAccountBootstrapService.ADMIN_EMAIL, adminPassword).andExpect(status().isOk());
        assertThat(users.findById(DemoAccountBootstrapService.OWNER_ID).orElseThrow().getRole())
                .isEqualTo(UserRole.BUSINESS_OWNER);
        assertThat(users.findById(DemoAccountBootstrapService.ADMIN_ID).orElseThrow().getRole())
                .isEqualTo(UserRole.ADMIN);
        assertThat(businesses.findById(DemoAccountBootstrapService.BUSINESS_ID).orElseThrow().getStatus())
                .isEqualTo(BusinessStatus.APPROVED);
    }

    @Test
    void enabledBootstrapIsIdempotentAndDoesNotTouchUnrelatedUsers() {
        User real = new User();
        real.setEmail("real-" + UUID.randomUUID() + "@example.test");
        real.setFullName("Real user");
        real.setRole(UserRole.CUSTOMER);
        real.setStatus(UserStatus.ACTIVE);
        real.setPasswordHash(passwords.encode("real-" + UUID.randomUUID()));
        real = users.saveAndFlush(real);
        String realHash = real.getPasswordHash();
        enable();
        bootstrap.apply();
        String ownerHash = users.findById(DemoAccountBootstrapService.OWNER_ID).orElseThrow().getPasswordHash();
        bootstrap.apply();
        assertThat(users.findById(DemoAccountBootstrapService.OWNER_ID).orElseThrow().getPasswordHash())
                .isEqualTo(ownerHash);
        assertThat(users.findById(real.getId())).hasValueSatisfying(user -> {
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getPasswordHash()).isEqualTo(realHash);
        });
    }

    @Test
    void missingRequiredDemoConfigurationFailsSafeAndLeavesAccountsDisabled() throws Exception {
        properties.setEnabled(true);
        properties.setOwnerPassword(ownerPassword);
        properties.setAdminPassword(null);
        bootstrap.apply();
        login(DemoAccountBootstrapService.OWNER_EMAIL, ownerPassword).andExpect(status().isUnauthorized());
        login(DemoAccountBootstrapService.ADMIN_EMAIL, adminPassword).andExpect(status().isUnauthorized());
    }

    @Test
    void restoredDemoAdminCannotOrderAndDemoOwnerCannotOrderOwnBusiness() throws Exception {
        enable();
        bootstrap.apply();
        String adminToken = accessToken(DemoAccountBootstrapService.ADMIN_EMAIL, adminPassword);
        String ownerToken = accessToken(DemoAccountBootstrapService.OWNER_EMAIL, ownerPassword);
        String order = "{\"businessId\":1,\"fulfillmentType\":\"PICKUP\",\"items\":[{\"productId\":1,\"quantity\":1}]}";
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(order))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(order))
                .andExpect(status().isNotFound());
    }

    private void enable() {
        properties.setEnabled(true);
        properties.setOwnerPassword(ownerPassword);
        properties.setAdminPassword(adminPassword);
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private String accessToken(String email, String password) throws Exception {
        String body = login(email, password).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }
}
