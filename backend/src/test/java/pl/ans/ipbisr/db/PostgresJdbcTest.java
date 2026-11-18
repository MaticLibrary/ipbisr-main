package pl.ans.ipbisr.db;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Wspólna baza dla testów bazy danych: prawdziwy PostgreSQL 16 w kontenerze Dockera.
 * Flyway uruchamia się sam i tworzy schemat z db/migration, więc testujemy ten sam kod SQL co na produkcji.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Tag("db")
abstract class PostgresJdbcTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");
}
