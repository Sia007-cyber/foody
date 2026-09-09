package com.foody.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class ProductionConfigTest {
    private MockEnvironment valid() {
        return new MockEnvironment().withProperty("foody.cors.allowed-origins", "https://foody.example.com")
                .withProperty("foody.storage.upload-dir", "/tmp/foody-production-test");
    }
    @Test void acceptsExplicitConfig() { assertThatCode(() -> new ProductionConfig(valid())).doesNotThrowAnyException(); }
    @Test void rejectsUnsafeOrigins() {
        for (String value : new String[]{"", "*", "http://localhost:5173", "https://app.example.com/", "https://app.example.com,"}) {
            assertThatThrownBy(() -> new ProductionConfig(valid().withProperty("foody.cors.allowed-origins", value)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void requiresAbsoluteUploadPath() {
        assertThatThrownBy(() -> new ProductionConfig(valid().withProperty("foody.storage.upload-dir", "./uploads")))
                .isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsMixedDevelopmentProfiles() {
        MockEnvironment env = valid(); env.setActiveProfiles("prod", "tc");
        assertThatThrownBy(() -> new ProductionConfig(env)).isInstanceOf(IllegalStateException.class);
    }
}
