package com.foody.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class ProductionConfigTest {
    private MockEnvironment valid() {
        MockEnvironment env = new MockEnvironment().withProperty("foody.cors.allowed-origins", "https://foody.example.com");
        env.setActiveProfiles("prod");
        return env;
    }
    @Test void acceptsExplicitConfig() { assertThatCode(() -> new ProductionConfig(valid())).doesNotThrowAnyException(); }
    @Test void rejectsUnsafeOrigins() {
        for (String value : new String[]{"", "*", "http://localhost:5173", "https://app.example.com/", "https://app.example.com,"}) {
            assertThatThrownBy(() -> new ProductionConfig(valid().withProperty("foody.cors.allowed-origins", value)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void rejectsMixedDevelopmentProfiles() {
        MockEnvironment env = valid(); env.setActiveProfiles("prod", "tc");
        assertThatThrownBy(() -> new ProductionConfig(env)).isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsMixedProductionProfiles() {
        MockEnvironment env = valid(); env.setActiveProfiles("prod", "vps");
        assertThatThrownBy(() -> new ProductionConfig(env)).isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsDemoAccountsInProduction() {
        MockEnvironment env = valid().withProperty("foody.demo-accounts.enabled", "true");
        assertThatThrownBy(() -> new ProductionConfig(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demo accounts");
    }
    @Test void vpsRequiresExternalStorageAndLoopbackPort() {
        MockEnvironment env = valid()
                .withProperty("foody.storage.upload-dir", "/var/lib/foody/uploads")
                .withProperty("server.address", "127.0.0.1")
                .withProperty("server.port", "8080");
        env.setActiveProfiles("vps");
        assertThatCode(() -> new ProductionConfig(env)).doesNotThrowAnyException();

        env.setProperty("server.address", "0.0.0.0");
        assertThatThrownBy(() -> new ProductionConfig(env)).isInstanceOf(IllegalStateException.class);
    }
}
