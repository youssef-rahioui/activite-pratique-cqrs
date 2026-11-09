package ma.youssef.bank;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import ma.youssef.bank.api.AccountCommandsController.AmountRequest;
import ma.youssef.bank.api.AccountCommandsController.CreateRequest;
import ma.youssef.bank.query.OperationSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@EnabledIfEnvironmentVariable(named = "RUN_EXTERNAL_TESTS", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "server.shutdown=immediate")
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
            assertThat((List<?>) response.get("operations")).hasSize(1);
        });

        CompletableFuture<Void> firstEvent = new CompletableFuture<>();
        CompletableFuture<List<ServerSentEvent<OperationSummary>>> stream = CompletableFuture.supplyAsync(() ->
                WebClient.create().get().uri(root + "/" + id + "/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM).retrieve()
                        .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<OperationSummary>>() {})
                        .doOnNext(event -> firstEvent.complete(null))
                        .take(2).collectList().block(Duration.ofSeconds(20)));
        firstEvent.orTimeout(10, java.util.concurrent.TimeUnit.SECONDS).join();
        var secondCredit = http.postForEntity(root + "/" + id + "/credits",
                new AmountRequest(new BigDecimal("5.00"), "MAD"), Map.class);
        assertThat(secondCredit.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(stream.join()).extracting(event -> event.data().type()).containsExactly("CREDIT", "CREDIT");

        var rejected = http.postForEntity(root + "/" + id + "/debits",
                new AmountRequest(new BigDecimal("1000.00"), "MAD"), Map.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        assertThat(http.getForEntity(root + "/missing", Map.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
