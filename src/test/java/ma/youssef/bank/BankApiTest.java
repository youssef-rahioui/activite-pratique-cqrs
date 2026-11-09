package ma.youssef.bank;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import ma.youssef.bank.api.AccountCommandsController.AmountRequest;
import ma.youssef.bank.api.AccountCommandsController.CreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "axon.axonserver.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:banktest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "axon.eventhandling.processors.account-projection.mode=subscribing"
})
class BankApiTest {
    @Autowired TestRestTemplate http;
    @LocalServerPort int port;

    @Test
    void commandsFlowThroughEventStoreIntoQueryProjection() {
        String root = "http://localhost:" + port + "/api/accounts";
        ResponseEntity<Map> creation = http.postForEntity(root,
                new CreateRequest(new BigDecimal("100.00"), "MAD"), Map.class);
        assertThat(creation.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String id = (String) creation.getBody().get("accountId");
        assertThat(id).isNotBlank();
        Map[] createdEvents = http.getForObject(root + "/" + id + "/events", Map[].class);
        assertThat(createdEvents).extracting(event -> event.get("type"))
                .containsExactly("Created", "Activated");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Map account = http.getForObject(root + "/" + id, Map.class);
            assertThat(account.get("status")).isEqualTo("ACTIVE");
        });

        ResponseEntity<Map> credit = http.postForEntity(root + "/" + id + "/credits",
                new AmountRequest(new BigDecimal("25.00"), "MAD"), Map.class);
        assertThat(credit.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Map account = http.getForObject(root + "/" + id, Map.class);
            assertThat(new BigDecimal(account.get("balance").toString())).isEqualByComparingTo("125.00");
            Map operations = http.getForObject(root + "/" + id + "/operations", Map.class);
            assertThat((java.util.List<?>) operations.get("operations")).hasSize(1);
        });

        ResponseEntity<Map> rejected = http.postForEntity(root + "/" + id + "/debits",
                new AmountRequest(new BigDecimal("200.00"), "MAD"), Map.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((java.util.List<?>) http.getForObject(root + "/" + id + "/operations", Map.class)
                .get("operations")).hasSize(1);
        assertThat(http.getForObject(root + "/" + id + "/events", Map[].class))
                .extracting(event -> event.get("type")).containsExactly("Created", "Activated", "Credited");

        Map latest = http.getForObject(root + "/" + id + "/state", Map.class);
        assertThat(new BigDecimal(latest.get("balance").toString())).isEqualByComparingTo("125.00");
        assertThat(latest.get("sequence")).isEqualTo(2);

        Map atCreation = http.getForObject(root + "/" + id + "/state?sequence=1", Map.class);
        assertThat(new BigDecimal(atCreation.get("balance").toString())).isEqualByComparingTo("100.00");
        assertThat(atCreation.get("status")).isEqualTo("ACTIVE");
        assertThat(atCreation.get("sequence")).isEqualTo(1);

        ResponseEntity<Map> missing = http.exchange(root + "/missing", HttpMethod.GET, HttpEntity.EMPTY, Map.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
