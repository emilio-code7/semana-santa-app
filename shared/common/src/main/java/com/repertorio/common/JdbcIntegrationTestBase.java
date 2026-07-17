package com.repertorio.common;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Base class for integration tests that connect to an external PostgreSQL instance.
 * NOT using Testcontainers — connects to a real PG at env-configurable JDBC URL.
 * Skips tests silently if PG is unreachable.
 *
 * <p>Subclasses must provide their own {@code @DynamicPropertySource} to wire
 * {@code spring.datasource.url/username/password} using the same env vars.</p>
 */
public abstract class JdbcIntegrationTestBase {

    private static boolean postgresAvailable = false;

    @BeforeAll
    static void checkPostgres() {
        if (postgresAvailable) return;
        var jdbcUrl = System.getenv().getOrDefault("IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5433/repertorio_db");
        var username = System.getenv().getOrDefault("IT_DATASOURCE_USERNAME", "postgres");
        var password = System.getenv().getOrDefault("IT_DATASOURCE_PASSWORD", "postgres");
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            postgresAvailable = true;
        } catch (Exception e) {
            postgresAvailable = false;
        }
        Assumptions.assumeTrue(postgresAvailable, "PostgreSQL not reachable — skipping integration tests");
    }

    /**
     * Subclasses can call this to check if the connectivity check passed.
     */
    protected static boolean isPostgresAvailable() {
        return postgresAvailable;
    }
}
