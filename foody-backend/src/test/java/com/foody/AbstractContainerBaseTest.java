package com.foody;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Spins up ONE real MySQL container, shared by every integration test class in the
 * suite (the "singleton container" pattern) — started once in a static initializer
 * and never explicitly stopped; Testcontainers' Ryuk reaper removes it when the JVM exits.
 *
 * This isn't just an optimization: every subclass gets the exact same JDBC URL/port,
 * so Spring's test ApplicationContext cache can reuse ONE context (and one Hikari pool)
 * across all integration test classes. The previous approach (JUnit-managed lifecycle via
 * @Testcontainers/@Container) restarted the container — on a new port — between test
 * classes, so Spring cached a stale context per class whose Hikari pool kept trying to
 * validate connections against an already-stopped container. That's the
 * "HikariPool-1 - Failed to validate connection ... No operations allowed after
 * connection closed" warning spam that can make `mvn test` hang indefinitely.
 */
@SpringBootTest
public abstract class AbstractContainerBaseTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("foody")
            .withUrlParam("createDatabaseIfNotExist", "true");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
}
