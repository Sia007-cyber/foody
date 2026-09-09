package com.foody;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("prod")
@TestPropertySource(properties = {
    "FOODY_JWT_SECRET=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
    "FOODY_CORS_ALLOWED_ORIGINS=https://foody.example.com",
    "FOODY_UPLOAD_DIR=/tmp/foody-production-test"
})
@AutoConfigureMockMvc
class ProductionStartupIntegrationTest extends AbstractContainerBaseTest {
    @Autowired Flyway flyway;
    @Autowired MockMvc mvc;
    @Test void productionStartsWithMigratedSchemaAndRestrictedCors() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("19");
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        mvc.perform(get("/api/businesses")).andExpect(status().isOk());
        mvc.perform(options("/api/businesses").header("Origin", "https://foody.example.com")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "https://foody.example.com"));
        mvc.perform(options("/api/businesses").header("Origin", "https://untrusted.example.com")
                .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
    }
    @Test void malformedRequestsAreControlled() throws Exception {
        mvc.perform(get("/api/businesses/not-a-number")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
