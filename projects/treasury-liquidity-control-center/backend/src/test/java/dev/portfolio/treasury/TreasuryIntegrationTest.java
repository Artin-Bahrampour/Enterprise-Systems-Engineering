package dev.portfolio.treasury;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=test", "app.demo-data.enabled=false"})
class TreasuryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("treasury_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.security.username", () -> "test-admin");
        registry.add("app.security.password", () -> "test-password");
    }

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("test-admin", "test-password");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void postingIsIdempotentAndBalanceIsUpdatedOnlyOnce() {
        HttpHeaders headers = authHeaders();
        ResponseEntity<Map> created = rest.exchange("http://localhost:" + port + "/api/accounts",
                HttpMethod.POST, new HttpEntity<>(Map.of("name", "Test Operating", "bankName", "Test Bank",
                        "currency", "NOK", "openingBalance", new BigDecimal("1000.0000"),
                        "overdraftLimit", new BigDecimal("100.0000")), headers), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String accountId = created.getBody().get("id").toString();

        Map<String, Object> payload = Map.of("direction", "DEBIT", "amount", new BigDecimal("125.0000"),
                "valueDate", LocalDate.now().toString(), "description", "Supplier invoice");
        headers.set("Idempotency-Key", "integration-key-1");
        ResponseEntity<Map> first = rest.exchange("http://localhost:" + port + "/api/accounts/" + accountId + "/movements",
                HttpMethod.POST, new HttpEntity<>(payload, headers), Map.class);
        ResponseEntity<Map> replay = rest.exchange("http://localhost:" + port + "/api/accounts/" + accountId + "/movements",
                HttpMethod.POST, new HttpEntity<>(payload, headers), Map.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replay.getBody().get("idempotentReplay")).isEqualTo(true);

        ResponseEntity<Map[]> accounts = rest.exchange("http://localhost:" + port + "/api/accounts",
                HttpMethod.GET, new HttpEntity<>(headers), Map[].class);
        Map account = java.util.Arrays.stream(accounts.getBody())
                .filter(a -> accountId.equals(a.get("id").toString())).findFirst().orElseThrow();
        assertThat(new BigDecimal(account.get("currentBalance").toString()))
                .isEqualByComparingTo("875.0000");
    }

    @Test
    void overdraftBreachReturnsUnprocessableEntity() {
        HttpHeaders headers = authHeaders();
        ResponseEntity<Map> created = rest.exchange("http://localhost:" + port + "/api/accounts",
                HttpMethod.POST, new HttpEntity<>(Map.of("name", "No Overdraft", "bankName", "Test Bank",
                        "currency", "NOK", "openingBalance", new BigDecimal("50.0000"),
                        "overdraftLimit", new BigDecimal("0.0000")), headers), Map.class);
        String id = created.getBody().get("id").toString();
        headers.set("Idempotency-Key", "overdraft-key");
        ResponseEntity<Map> result = rest.exchange("http://localhost:" + port + "/api/accounts/" + id + "/movements",
                HttpMethod.POST, new HttpEntity<>(Map.of("direction", "DEBIT", "amount", new BigDecimal("60.0000"),
                        "valueDate", LocalDate.now().toString(), "description", "Too large"), headers), Map.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
