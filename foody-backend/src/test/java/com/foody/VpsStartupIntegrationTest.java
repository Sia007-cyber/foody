package com.foody;

import com.foody.common.storage.LocalUploadStorage;
import com.foody.common.storage.UploadStorage;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("vps")
@TestPropertySource(properties = {
    "FOODY_JWT_SECRET=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
    "FOODY_CORS_ALLOWED_ORIGINS=https://foody.example.com",
    "FOODY_STORAGE_LOCAL_PATH=/tmp/foody-vps-startup-test"
})
class VpsStartupIntegrationTest extends AbstractContainerBaseTest {
    @Autowired Flyway flyway;
    @Autowired UploadStorage uploadStorage;
    @Autowired HikariDataSource dataSource;
    @Autowired Environment environment;

    @Test
    void vpsProfileStartsWithLocalStorageAndConservativeRuntimeSettings() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("24");
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(uploadStorage).isInstanceOf(LocalUploadStorage.class);
        assertThat(dataSource.getMaximumPoolSize()).isEqualTo(4);
        assertThat(dataSource.getMinimumIdle()).isEqualTo(1);
        assertThat(environment.getProperty("server.address")).isEqualTo("127.0.0.1");
        assertThat(environment.getProperty("server.port")).isEqualTo("8080");
        assertThat(environment.getProperty("foody.demo-accounts.enabled", Boolean.class)).isFalse();
    }
}
