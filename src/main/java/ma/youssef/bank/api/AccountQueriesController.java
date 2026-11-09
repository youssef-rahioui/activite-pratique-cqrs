package ma.youssef.bank.api;

import java.util.concurrent.CompletableFuture;
import ma.youssef.bank.query.AccountList;
import ma.youssef.bank.query.AccountQuery;
import ma.youssef.bank.query.AccountSummary;
import ma.youssef.bank.query.OperationList;
import ma.youssef.bank.query.OperationSummary;
import ma.youssef.bank.query.StateSnapshot;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/accounts")
public class AccountQueriesController {
    private final QueryGateway queries;

    public AccountQueriesController(QueryGateway queries) {
        this.queries = queries;
    }

    @GetMapping
    public CompletableFuture<AccountList> all() {
        return queries.query(new AccountQuery.All(), ResponseTypes.instanceOf(AccountList.class));
    }

    @GetMapping("/{id}")
    public CompletableFuture<AccountSummary> one(@PathVariable String id) {
        return queries.query(new AccountQuery.ById(id), ResponseTypes.instanceOf(AccountSummary.class));
    }

    @GetMapping("/{id}/operations")
    public CompletableFuture<OperationList> operations(@PathVariable String id) {
        return queries.query(new AccountQuery.Operations(id), ResponseTypes.instanceOf(OperationList.class));
    }

    @GetMapping("/{id}/state")
    public CompletableFuture<StateSnapshot> stateAt(@PathVariable String id,
                                                    @RequestParam(required = false) Long sequence) {
        return queries.query(new AccountQuery.StateAt(id, sequence), ResponseTypes.instanceOf(StateSnapshot.class));
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<OperationSummary>> watch(@PathVariable String id) {
        SubscriptionQueryResult<OperationList, OperationSummary> subscription = queries.subscriptionQuery(
                new AccountQuery.Watch(id),
                ResponseTypes.instanceOf(OperationList.class),
                ResponseTypes.instanceOf(OperationSummary.class));
        Flux<OperationSummary> history = subscription.initialResult().flatMapIterable(OperationList::operations);
        return Flux.concat(history, subscription.updates())
                .map(operation -> ServerSentEvent.<OperationSummary>builder().event("operation").data(operation).build())
                .doFinally(signal -> subscription.close());
    }
}
