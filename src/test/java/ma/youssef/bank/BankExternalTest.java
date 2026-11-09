package ma.youssef.bank;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import ma.youssef.bank.api.AccountCommandsController.AmountRequest;
import ma.youssef.bank.api.AccountCommandsController.CreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@EnabledIfEnvironmentVariable(named = "RUN_EXTERNAL_TESTS", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BankExternalTest {
    @Autowired TestRestTemplate http;
    @LocalServerPort int port;

    @Test
    void writesToAxonServerAndProjectsIntoPostgres() {
        String root = "http://localhost:" + port + "/api/accounts";
        var created = http.postForEntity(root, new CreateRequest(new BigDecimal("50.00"), "MAD"), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String id = (String) created.getBody().get("accountId");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(http.getForObject(root + "/" + id, Map.class).get("status")).isEqualTo("ACTIVE"));

        var credited = http.postForEntity(root + "/" + id + "/credits",
                new AmountRequest(new BigDecimal("10.00"), "MAD"), Map.class);
        assertThat(credited.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Map account = http.getForObject(root + "/" + id, Map.class);
            assertThat(new BigDecimal(account.get("balance").toString())).isEqualByComparingTo("60.00");
            Map response = http.getForObject(root + "/" + id + "/operations", Map.class);
            assertThat((java.util.List<?>) response.get("operations")).hasSize(1);
        });
    }
}
