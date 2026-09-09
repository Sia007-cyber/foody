package com.foody.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.AbstractContainerBaseTest;
import com.foody.auth.dto.TokenResponse;
import com.foody.auth.entity.RefreshTokenSession;
import com.foody.auth.repository.RefreshTokenSessionRepository;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
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
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends AbstractContainerBaseTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired RefreshTokenSessionRepository refreshTokenRepository;

    private record LoggedInAccount(User user, TokenResponse tokens) {}

    private LoggedInAccount loginActiveAccount(UserRole role) throws Exception {
        // Provision isolated accounts directly so ADMIN is covered without enabling admin signup.
        User user = new User();
        user.setEmail("eligibility_" + UUID.randomUUID() + "@foody.test");
        user.setFullName("Account eligibility test");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user = userRepository.saveAndFlush(user);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", user.getEmail(), "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        TokenResponse tokens = objectMapper.readValue(response, TokenResponse.class);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.role").value(role.name()));
        return new LoggedInAccount(user, tokens);
    }

    private void assertAccountTokensRejected(TokenResponse tokens) throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", tokens.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void activeAccount_canAccessAndRefresh(UserRole role) throws Exception {
        LoggedInAccount account = loginActiveAccount(role);
        String response = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", account.tokens().refreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        TokenResponse refreshed = objectMapper.readValue(response, TokenResponse.class);
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + refreshed.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.user().getId()))
                .andExpect(jsonPath("$.role").value(role.name()));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void suspendedAfterLogin_existingAccessAndRefreshAreRejected(UserRole role) throws Exception {
        LoggedInAccount account = loginActiveAccount(role);
        account.user().setStatus(UserStatus.SUSPENDED);
        // No enclosing test transaction: commit the status before subsequent HTTP requests.
        userRepository.saveAndFlush(account.user());

        assertAccountTokensRejected(account.tokens());
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void deletedAfterLogin_existingAccessAndRefreshAreRejected(UserRole role) throws Exception {
        LoggedInAccount account = loginActiveAccount(role);
        // These accounts have no business/order/wallet records, so deletion respects FKs.
        userRepository.deleteById(account.user().getId());

        assertAccountTokensRejected(account.tokens());
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void refreshToken_cannotAuthenticateProtectedRequest(UserRole role) throws Exception {
        LoggedInAccount account = loginActiveAccount(role);
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + account.tokens().refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    private String registerAndGetAccessToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email, "password", password, "fullName", "Test User", "phone", "123",
                "role", "CUSTOMER"));
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void register_login_refresh_me_logout_flow() throws Exception {
        String email = "flow_" + System.nanoTime() + "@foody.test";
        String password = "password123";
        String access = registerAndGetAccessToken(email, password);

        // GET /api/users/me with the token
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        // Login returns a fresh token pair
        String loginBody = objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // Refresh: capture refresh token then use it
        String regResp = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", email + "_2", "password", password, "fullName", "U2",
                                        "role", "CUSTOMER"))))
                .andReturn().getResponse().getContentAsString();
        String refresh = objectMapper.readTree(regResp).get("refreshToken").asText();
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // Logout revokes the presented refresh session.
        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isOk());
        refreshRejected(refresh);
    }

    @Test
    void refresh_rotatesAndRejectsOldToken() throws Exception {
        LoggedInAccount account = loginActiveAccount(UserRole.CUSTOMER);
        String oldToken = account.tokens().refreshToken();
        TokenResponse rotated = refreshSuccessfully(oldToken);

        org.assertj.core.api.Assertions.assertThat(rotated.refreshToken()).isNotEqualTo(oldToken);
        refreshRejected(oldToken);
        refreshSuccessfully(rotated.refreshToken());
    }

    @Test
    void concurrentRefresh_onlyOneRotationSucceeds() throws Exception {
        String refreshToken = loginActiveAccount(UserRole.CUSTOMER).tokens().refreshToken();
        String body = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> request = () -> {
                ready.countDown();
                start.await();
                return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                                .content(body)).andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(request);
            Future<Integer> second = executor.submit(request);
            ready.await();
            start.countDown();
            List<Integer> statuses = List.of(first.get(), second.get());
            org.assertj.core.api.Assertions.assertThat(statuses).containsExactlyInAnyOrder(200, 401);
        }
    }

    @Test
    void passwordChange_revokesAllExistingRefreshSessions() throws Exception {
        LoggedInAccount first = loginActiveAccount(UserRole.CUSTOMER);
        String secondLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", first.user().getEmail(), "password", "password123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        TokenResponse second = objectMapper.readValue(secondLogin, TokenResponse.class);

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + first.tokens().accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "newPassword123"))))
                .andExpect(status().isOk());

        refreshRejected(first.tokens().refreshToken());
        refreshRejected(second.refreshToken());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", first.user().getEmail(), "password", "newPassword123"))))
                .andExpect(status().isOk());
    }

    @Test
    void suspendedAccount_cannotLogin() throws Exception {
        LoggedInAccount account = loginActiveAccount(UserRole.CUSTOMER);
        account.user().setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(account.user());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", account.user().getEmail(), "password", "password123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void logout_revokesRefreshToken() throws Exception {
        String refreshToken = loginActiveAccount(UserRole.CUSTOMER).tokens().refreshToken();
        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk());
        refreshRejected(refreshToken);
    }

    @Test
    void persistedExpiry_isEnforced() throws Exception {
        String refreshToken = loginActiveAccount(UserRole.CUSTOMER).tokens().refreshToken();
        RefreshTokenSession session = refreshTokenRepository.findAll().stream()
                .max(java.util.Comparator.comparing(RefreshTokenSession::getId)).orElseThrow();
        session.setExpiresAt(Instant.now().minusSeconds(1));
        refreshTokenRepository.saveAndFlush(session);
        refreshRejected(refreshToken);
    }

    private TokenResponse refreshSuccessfully(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }

    private void refreshRejected(String refreshToken) throws Exception {
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void protectedEndpoint_rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateEmail_returnsConflictWithCode() throws Exception {
        String email = "dup_" + System.nanoTime() + "@foody.test";
        registerAndGetAccessToken(email, "password123");

        String body = objectMapper.writeValueAsString(
                java.util.Map.of("email", email, "password", "password123", "fullName", "Dup", "role", "CUSTOMER"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_ALREADY_EXISTS"));
    }

    @Test
    void register_businessOwner_createsBusinessOwnerAccount() throws Exception {
        String email = "owner_" + System.nanoTime() + "@foody.test";
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email, "password", "password123", "fullName", "Owner", "role", "BUSINESS_OWNER"));
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String access = objectMapper.readTree(response).get("accessToken").asText();

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BUSINESS_OWNER"));
    }

    @Test
    void register_adminRole_isRejected() throws Exception {
        String email = "admin_" + System.nanoTime() + "@foody.test";
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email, "password", "password123", "fullName", "Admin", "role", "ADMIN"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchMe_updatesFields() throws Exception {
        String access = registerAndGetAccessToken("patch_" + System.nanoTime() + "@foody.test", "password123");
        String patch = objectMapper.writeValueAsString(
                java.util.Map.of("fullName", "Renamed", "phone", "999"));
        mockMvc.perform(patch("/api/users/me").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON).content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Renamed"))
                .andExpect(jsonPath("$.phone").value("999"));
    }

    @Test
    void seededBusiness_isViewable() throws Exception {
        // V2 migration seeds business id=1 (APPROVED). It is publicly readable.
        mockMvc.perform(get("/api/businesses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("کافه سان‌رایز"))
                .andExpect(jsonPath("$.businessType").value("CAFE"));
    }

    @Test
    void seededBusiness_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/businesses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
