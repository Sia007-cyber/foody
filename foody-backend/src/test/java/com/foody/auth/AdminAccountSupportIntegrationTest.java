package com.foody.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.AbstractContainerBaseTest;
import com.foody.auth.dto.TokenResponse;
import com.foody.auth.entity.ImpersonationSession;
import com.foody.auth.repository.ImpersonationSessionRepository;
import com.foody.auth.repository.RefreshTokenSessionRepository;
import com.foody.auth.security.JwtService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AdminAccountSupportIntegrationTest extends AbstractContainerBaseTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService jwt;
    @Autowired ImpersonationSessionRepository impersonations;
    @Autowired RefreshTokenSessionRepository refreshSessions;

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"CUSTOMER", "BUSINESS_OWNER"})
    void adminCanImpersonateEligibleUsers_andClaimsAndAuditIdentifyAdmin(UserRole role) throws Exception {
        User admin = user(UserRole.ADMIN, "adminPassword123");
        User target = user(role, "targetPassword123");
        TokenResponse tokens = impersonate(admin, target, status().isOk());

        var claims = jwt.parse(tokens.accessToken());
        assertThat(jwt.isImpersonation(claims)).isTrue();
        assertThat(jwt.getInitiatingAdminId(claims)).isEqualTo(admin.getId());
        assertThat(jwt.getUserId(claims)).isEqualTo(target.getId());
        assertThat(tokens.impersonation().sessionId()).isEqualTo(jwt.getImpersonationSessionId(claims));
        ImpersonationSession audit = impersonations.findById(tokens.impersonation().sessionId()).orElseThrow();
        assertThat(audit.getAdmin().getId()).isEqualTo(admin.getId());
        assertThat(audit.getTarget().getId()).isEqualTo(target.getId());
        assertThat(audit.getStartedAt()).isNotNull();

        mvc.perform(post("/api/admin/users/{id}/impersonate", user(UserRole.CUSTOMER, "password123").getId())
                .header("Authorization", bearer(tokens.accessToken())))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/auth/impersonation/exit").header("Authorization", bearer(tokens.accessToken())))
                .andExpect(status().isOk());
        assertThat(impersonations.findById(audit.getId()).orElseThrow().getEndedAt()).isNotNull();
        mvc.perform(get("/api/users/me").header("Authorization", bearer(tokens.accessToken())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", tokens.refreshToken()))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(jwt.generateAccessToken(admin))))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotImpersonateAdmin() throws Exception {
        impersonate(user(UserRole.ADMIN, "password123"), user(UserRole.ADMIN, "password123"), status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"CUSTOMER", "BUSINESS_OWNER"})
    void nonAdminCannotImpersonateOrResetPassword(UserRole role) throws Exception {
        User actor = user(role, "password123");
        User target = user(UserRole.CUSTOMER, "password123");
        mvc.perform(post("/api/admin/users/{id}/impersonate", target.getId()).header("Authorization", bearer(jwt.generateAccessToken(actor))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/users/{id}/password-reset", target.getId())
                .header("Authorization", bearer(jwt.generateAccessToken(actor))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"replacement123\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"CUSTOMER", "BUSINESS_OWNER"})
    void adminResetHashesReplacementAndRevokesRefreshSessions(UserRole role) throws Exception {
        User admin = user(UserRole.ADMIN, "adminPassword123");
        User target = user(role, "oldPassword123");
        TokenResponse oldTokens = login(target, "oldPassword123");

        mvc.perform(post("/api/admin/users/{id}/password-reset", target.getId())
                .header("Authorization", bearer(jwt.generateAccessToken(admin))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"newPassword123\"}"))
                .andExpect(status().isOk());

        String stored = users.findById(target.getId()).orElseThrow().getPasswordHash();
        assertThat(stored).isNotEqualTo("newPassword123");
        assertThat(passwords.matches("newPassword123", stored)).isTrue();
        loginExpecting(target, "oldPassword123", 401);
        loginExpecting(target, "newPassword123", 200);
        assertThat(refreshSessions.findAll().stream()
                .filter(s -> s.getUser().getId().equals(target.getId()) && s.getRevokedAt() == null).count()).isEqualTo(1);
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", oldTokens.refreshToken()))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users/{id}", target.getId()).header("Authorization", bearer(jwt.generateAccessToken(admin))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    private User user(UserRole role, String password) {
        User user = new User();
        user.setEmail(UUID.randomUUID() + "@support.test"); user.setFullName(role + " support target");
        user.setPasswordHash(passwords.encode(password)); user.setRole(role); user.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(user);
    }

    private TokenResponse impersonate(User admin, User target,
            org.springframework.test.web.servlet.ResultMatcher expected) throws Exception {
        var result = mvc.perform(post("/api/admin/users/{id}/impersonate", target.getId())
                .header("Authorization", bearer(jwt.generateAccessToken(admin)))).andExpect(expected).andReturn();
        return result.getResponse().getStatus() == 200
                ? json.readValue(result.getResponse().getContentAsString(), TokenResponse.class) : null;
    }

    private TokenResponse login(User user, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("identifier", user.getEmail(), "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readValue(body, TokenResponse.class);
    }

    private void loginExpecting(User user, String password, int expected) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("identifier", user.getEmail(), "password", password))))
                .andExpect(status().is(expected));
    }

    private static String bearer(String token) { return "Bearer " + token; }
}
