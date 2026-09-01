package com.eyup.library.base;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One PostgreSQL container for the whole test suite.
 *
 * <p>Started on first use and left running, so the suite pays the startup cost
 * once instead of per test class; Testcontainers' reaper removes it when the JVM
 * exits. Both {@code AbstractDataJpaTest} and {@code AbstractRestControllerTest}
 * point their datasource at it, so every test runs against real PostgreSQL with
 * the Flyway migrations applied.</p>
 */
final class PostgresTestContainer {

    private static final PostgreSQLContainer INSTANCE = new PostgreSQLContainer("postgres:17-alpine");

    static {
        INSTANCE.start();
    }

    private PostgresTestContainer() {
    }

    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", INSTANCE::getUsername);
        registry.add("spring.datasource.password", INSTANCE::getPassword);
    }

}
