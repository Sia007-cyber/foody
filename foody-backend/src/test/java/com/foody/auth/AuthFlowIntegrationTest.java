package com.foody.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.AbstractContainerBaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends AbstractContainerBaseTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String registerAndGetAccessToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email, "password", password, "fullName", "Test User", "phone", "123"));
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
                                java.util.Map.of("email", email + "_2", "password", password, "fullName", "U2"))))
                .andReturn().getResponse().getContentAsString();
        String refresh = objectMapper.readTree(regResp).get("refreshToken").asText();
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // Logout is a no-op 200
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
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
                java.util.Map.of("email", email, "password", "password123", "fullName", "Dup"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_ALREADY_EXISTS"));
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
                .andExpect(jsonPath("$.name").value("Cafe Sunrise"))
                .andExpect(jsonPath("$.businessType").value("CAFE"));
    }

    @Test
    void seededBusiness_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/businesses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
