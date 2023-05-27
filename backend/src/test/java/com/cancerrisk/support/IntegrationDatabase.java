package com.cancerrisk.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/** Shares one PostgreSQL instance across all integration test classes. */
public final class IntegrationDatabase {

    private static PostgreSQLContainer<?> container;

    private IntegrationDatabase() {
    }

    static String externalUrl() {
        String url = System.getenv("TEST_DATABASE_URL");
        return url == null || url.isBlank() ? null : url;
    }

    public static synchronized void register(DynamicPropertyRegistry registry) {
        String external = externalUrl();
        if (external != null) {
            registry.add("spring.datasource.url", () -> external);
            registry.add("spring.datasource.username", () -> env("TEST_DATABASE_USERNAME", "cancer_risk"));
            registry.add("spring.datasource.password", () -> env("TEST_DATABASE_PASSWORD", "cancer_risk"));
            return;
        }
        if (container == null) {
            container = new PostgreSQLContainer<>("postgres:15.3")
                    .withDatabaseName("cancer_risk")
                    .withUsername("cancer_risk")
                    .withPassword("cancer_risk");
            container.start();
        }
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
