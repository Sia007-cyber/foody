package com.foody.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.AbstractContainerBaseTest;
import com.foody.admin.entity.AdminRoleAction;
import com.foody.admin.repository.AdminRoleAuditRepository;
import com.foody.auth.dto.TokenResponse;
import com.foody.auth.security.JwtService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class PrimaryAdminRoleManagementIntegrationTest extends AbstractContainerBaseTest {
    private static final String PASSWORD = "testPassword123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired AdminRoleAuditRepository audits;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService jwt;

    @Test
    void primaryAdminCanGrantAndRevoke_withAuditAndRestoredBaseRole() throws Exception {
        User primary = primaryAdmin();
        User target = user(UserRole.BUSINESS_OWNER, UserStatus.ACTIVE);

        mvc.perform(patch("/api/admin/users/{id}/grant-admin", target.getId()).headers(auth(primary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.primaryAdmin").value(false));
        User granted = users.findById(target.getId()).orElseThrow();
        assertThat(granted.getAdminBaseRole()).isEqualTo(UserRole.BUSINESS_OWNER);
        assertAudit(target.getId(), primary.getId(), UserRole.BUSINESS_OWNER, UserRole.ADMIN,
                AdminRoleAction.GRANT_ADMIN);

        mvc.perform(patch("/api/admin/users/{id}/revoke-admin", target.getId()).headers(auth(primary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BUSINESS_OWNER"));
        assertThat(users.findById(target.getId()).orElseThrow().getAdminBaseRole()).isNull();
        assertAudit(target.getId(), primary.getId(), UserRole.ADMIN, UserRole.BUSINESS_OWNER,
                AdminRoleAction.REVOKE_ADMIN);
    }

    @Test
    void ordinaryAdminCannotGrantOrRevoke() throws Exception {
        User ordinaryAdmin = user(UserRole.ADMIN, UserStatus.ACTIVE);
        User customer = user(UserRole.CUSTOMER, UserStatus.ACTIVE);
        User anotherAdmin = user(UserRole.ADMIN, UserStatus.ACTIVE);

        mvc.perform(patch("/api/admin/users/{id}/grant-admin", customer.getId()).headers(auth(ordinaryAdmin)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/users/{id}/revoke-admin", anotherAdmin.getId()).headers(auth(ordinaryAdmin)))
                .andExpect(status().isForbidden());
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(users.findById(anotherAdmin.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"CUSTOMER", "BUSINESS_OWNER"})
    void nonAdminsCannotManageAdministratorRoles(UserRole role) throws Exception {
        User actor = user(role, UserStatus.ACTIVE);
        User target = user(UserRole.CUSTOMER, UserStatus.ACTIVE);
        mvc.perform(patch("/api/admin/users/{id}/grant-admin", target.getId()).headers(auth(actor)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/users/{id}/revoke-admin", target.getId()).headers(auth(actor)))
                .andExpect(status().isForbidden());
    }

    @Test
    void primaryAdminCannotBeDemotedOrControlledByOrdinaryAdminActions() throws Exception {
        User primary = primaryAdmin();
        User ordinaryAdmin = user(UserRole.ADMIN, UserStatus.ACTIVE);

        mvc.perform(patch("/api/admin/users/{id}/revoke-admin", primary.getId()).headers(auth(primary)))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/admin/users/{id}/suspend", primary.getId()).headers(auth(ordinaryAdmin)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/users/{id}/password-reset", primary.getId()).headers(auth(ordinaryAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"replacement123\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/users/{id}/impersonate", primary.getId()).headers(auth(ordinaryAdmin)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/users/{id}", primary.getId()).headers(auth(ordinaryAdmin)))
                .andExpect(status().isMethodNotAllowed());
        User unchanged = users.findById(primary.getId()).orElseThrow();
        assertThat(unchanged.isPrimaryAdmin()).isTrue();
        assertThat(unchanged.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(unchanged.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void invalidTargetsAndRepeatedTransitionsFailWithPreciseStatuses() throws Exception {
        User primary = primaryAdmin();
        User suspended = user(UserRole.CUSTOMER, UserStatus.SUSPENDED);
        User customer = user(UserRole.CUSTOMER, UserStatus.ACTIVE);

        mvc.perform(patch("/api/admin/users/{id}/grant-admin", Long.MAX_VALUE).headers(auth(primary)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/admin/users/{id}/grant-admin", suspended.getId()).headers(auth(primary)))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/admin/users/{id}/revoke-admin", customer.getId()).headers(auth(primary)))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/admin/users/{id}/grant-admin", customer.getId()).headers(auth(primary)))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/admin/users/{id}/grant-admin", customer.getId()).headers(auth(primary)))
                .andExpect(status().isConflict());
    }

    @Test
    void roleChangesRevokeRefreshTokensAndOldAccessUsesCurrentDatabaseRole() throws Exception {
        User primary = primaryAdmin();
        User target = user(UserRole.CUSTOMER, UserStatus.ACTIVE);
        TokenResponse beforeGrant = login(target);

        mvc.perform(patch("/api/admin/users/{id}/grant-admin", target.getId()).headers(auth(primary)))
                .andExpect(status().isOk());
        refreshRejected(beforeGrant.refreshToken());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(beforeGrant.accessToken())))
                .andExpect(status().isOk());

        TokenResponse privileged = login(users.findById(target.getId()).orElseThrow());
        mvc.perform(patch("/api/admin/users/{id}/revoke-admin", target.getId()).headers(auth(primary)))
                .andExpect(status().isOk());
        refreshRejected(privileged.refreshToken());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(privileged.accessToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokingAdminEndsItsActiveImpersonationSessions() throws Exception {
        User primary = primaryAdmin();
        User admin = user(UserRole.CUSTOMER, UserStatus.ACTIVE);
        User victim = user(UserRole.CUSTOMER, UserStatus.ACTIVE);
        mvc.perform(patch("/api/admin/users/{id}/grant-admin", admin.getId()).headers(auth(primary)))
                .andExpect(status().isOk());
        User promoted = users.findById(admin.getId()).orElseThrow();
        String response = mvc.perform(post("/api/admin/users/{id}/impersonate", victim.getId()).headers(auth(promoted)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        TokenResponse impersonation = json.readValue(response, TokenResponse.class);

        mvc.perform(patch("/api/admin/users/{id}/revoke-admin", admin.getId()).headers(auth(primary)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/users/me").header("Authorization", bearer(impersonation.accessToken())))
                .andExpect(status().isUnauthorized());
        refreshRejected(impersonation.refreshToken());
    }

    @Test
    void registrationCannotCreateAdminOrPrimaryAdmin() throws Exception {
        String unique = UUID.randomUUID().toString();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", unique + "@registration.test", "phone", phone(unique),
                                "password", PASSWORD, "fullName", "Registration Test", "role", "ADMIN"))))
                .andExpect(status().isBadRequest());

        String customerJson = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", "customer-" + unique + "@registration.test", "phone", phone("customer-" + unique),
                                "password", PASSWORD, "fullName", "Registration Test", "role", "CUSTOMER"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String access = json.readTree(customerJson).get("accessToken").asText();
        mvc.perform(get("/api/users/me").header("Authorization", bearer(access)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.primaryAdmin").value(false));
    }

    @Test
    void concurrentGrantIsSerializedAndWritesOneAuditEntry() throws Exception {
        User primary = primaryAdmin();
        User target = user(UserRole.CUSTOMER, UserStatus.ACTIVE);
        long before = audits.countByTargetIdAndActionType(target.getId(), AdminRoleAction.GRANT_ADMIN);
        String authorization = bearer(jwt.generateAccessToken(primary));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> request = () -> {
                ready.countDown();
                start.await();
                return mvc.perform(patch("/api/admin/users/{id}/grant-admin", target.getId())
                                .header("Authorization", authorization))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(request);
            Future<Integer> second = executor.submit(request);
            ready.await();
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
        }

        assertThat(audits.countByTargetIdAndActionType(target.getId(), AdminRoleAction.GRANT_ADMIN))
                .isEqualTo(before + 1);
    }

    private void assertAudit(Long targetId, Long actorId, UserRole previous, UserRole next,
                             AdminRoleAction action) {
        var audit = audits.findTopByTargetIdOrderByIdDesc(targetId).orElseThrow();
        assertThat(audit.getActor().getId()).isEqualTo(actorId);
        assertThat(audit.getTarget().getId()).isEqualTo(targetId);
        assertThat(audit.getPreviousRole()).isEqualTo(previous);
        assertThat(audit.getNewRole()).isEqualTo(next);
        assertThat(audit.getActionType()).isEqualTo(action);
        assertThat(audit.getCreatedAt()).isNotNull();
    }

    private User primaryAdmin() {
        return users.findByPrimaryAdminTrue().orElseGet(() -> {
            String unique = UUID.randomUUID().toString();
            User primary = new User();
            primary.setEmail(unique + "@role-management.test");
            primary.setPhone(phone(unique));
            primary.setFullName("Primary admin role test");
            primary.setPasswordHash(passwords.encode(PASSWORD));
            primary.setRole(UserRole.ADMIN);
            primary.setStatus(UserStatus.ACTIVE);
            primary.setPrimaryAdmin(true);
            return users.saveAndFlush(primary);
        });
    }

    private User user(UserRole role, UserStatus status) {
        String unique = UUID.randomUUID().toString();
        User user = new User();
        user.setEmail(unique + "@role-management.test");
        user.setPhone(phone(unique));
        user.setFullName(role + " role test");
        user.setPasswordHash(passwords.encode(PASSWORD));
        user.setRole(role);
        user.setStatus(status);
        if (role == UserRole.ADMIN) user.setAdminBaseRole(UserRole.CUSTOMER);
        return users.saveAndFlush(user);
    }

    private TokenResponse login(User user) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("identifier", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readValue(response, TokenResponse.class);
    }

    private void refreshRejected(String token) throws Exception {
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", token))))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.http.HttpHeaders auth(User user) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(jwt.generateAccessToken(user));
        return headers;
    }

    private static String bearer(String token) { return "Bearer " + token; }
    private static String phone(String value) {
        return "09" + String.format("%09d", Math.floorMod(value.hashCode(), 1_000_000_000));
    }
}
