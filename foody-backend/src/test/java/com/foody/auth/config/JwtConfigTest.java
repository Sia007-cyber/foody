package com.foody.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class JwtConfigTest {

    private static final String VALID_PRODUCTION_SECRET =
            Base64.getEncoder().encodeToString(new byte[32]);
    private static final String WEAK_SECRET =
            Base64.getEncoder().encodeToString(new byte[31]);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void productionWithStrongBase64Secret_createsSigningKey() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "FOODY_JWT_SECRET=" + VALID_PRODUCTION_SECRET)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecretKey.class);
                    assertThat(context.getBean(SecretKey.class).getEncoded()).hasSize(32);
                });
    }

    @Test
    void productionWithoutSecret_failsStartup() {
        assertStartupFails("FOODY_JWT_SECRET is required in production and must not be blank",
                "spring.profiles.active=prod");
    }

    @Test
    void productionWithBlankSecret_failsStartup() {
        assertStartupFails("FOODY_JWT_SECRET is required in production and must not be blank",
                "spring.profiles.active=prod", "FOODY_JWT_SECRET=");
    }

    @Test
    void productionWithCommittedDevelopmentSecret_failsStartup() {
        assertStartupFails("Production JWT secret must not use the committed development default",
                "spring.profiles.active=prod", "FOODY_JWT_SECRET=" + JwtConfig.DEVELOPMENT_SECRET);
    }

    @Test
    void productionWithMalformedBase64_failsStartup() {
        assertStartupFails("FOODY_JWT_SECRET must be valid Base64",
                "spring.profiles.active=prod", "FOODY_JWT_SECRET=not-valid-base64!");
    }

    @Test
    void productionWithKeyBelowHmacMinimum_failsStartup() {
        assertStartupFails(
                "FOODY_JWT_SECRET must decode to at least 256 bits (32 bytes) for HMAC JWT signing",
                "spring.profiles.active=prod", "FOODY_JWT_SECRET=" + WEAK_SECRET);
    }

    @Test
    void localProfileUsesDevelopmentFallback() {
        contextRunner
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecretKey.class);
                    assertThat(context.getBean(JwtProperties.class).getSecret())
                            .isEqualTo(JwtConfig.DEVELOPMENT_SECRET);
                });
    }

    @Test
    void testcontainersProfileUsesDevelopmentFallback() {
        contextRunner
                .withPropertyValues("spring.profiles.active=tc")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecretKey.class);
                    assertThat(context.getBean(JwtProperties.class).getSecret())
                            .isEqualTo(JwtConfig.DEVELOPMENT_SECRET);
                });
    }

    private void assertStartupFails(String expectedMessage, String... properties) {
        contextRunner.withPropertyValues(properties).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(expectedMessage);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    @Import(JwtConfig.class)
    static class TestConfiguration {
    }
}
