package ma.youssef.bank.query;

import java.math.BigDecimal;
import ma.youssef.bank.domain.AccountEvent;
import ma.youssef.bank.domain.AccountStatus;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class AccountQueryHandler {
    private final AccountRepository accounts;
    private final OperationRepository operations;
    private final EventStore events;

    public AccountQueryHandler(AccountRepository accounts, OperationRepository operations, EventStore events) {
        this.accounts = accounts;
        this.operations = operations;
        this.events = events;
    }

    @QueryHandler
    public AccountList handle(AccountQuery.All query) {
        return new AccountList(accounts.findAll().stream().map(AccountSummary::from).toList());
    }

    @QueryHandler
    public AccountSummary handle(AccountQuery.ById query) {
        return AccountSummary.from(accounts.findById(query.accountId())
                .orElseThrow(() -> new AccountNotFoundException(query.accountId())));
    }

    @QueryHandler
    public OperationList handle(AccountQuery.Operations query) {
        if (!accounts.existsById(query.accountId())) {
            throw new AccountNotFoundException(query.accountId());
        }
        return new OperationList(operations.findByAccountIdOrderByOccurredAtAsc(query.accountId()).stream()
                .map(OperationSummary::from).toList());
    }

    @QueryHandler
    public OperationList handle(AccountQuery.Watch query) {
        return handle(new AccountQuery.Operations(query.accountId()));
    }

    @QueryHandler
    public StateSnapshot handle(AccountQuery.StateAt query) {
        if (query.sequence() != null && query.sequence() < 0) {
            throw new IllegalArgumentException("Sequence must be zero or greater");
        }
        DomainEventStream stream = events.readEvents(query.accountId());
        BigDecimal balance = null;
        String currency = null;
        AccountStatus status = AccountStatus.CREATED;
        long sequence = 0;
        boolean seen = false;
        while (stream.hasNext()) {
            DomainEventMessage<?> message = stream.next();
            if (query.sequence() != null && message.getSequenceNumber() > query.sequence()) {
                break;
            }
            Object payload = message.getPayload();
            if (payload instanceof AccountEvent.Created event) {
                balance = event.initialBalance();
                currency = event.currency();
                status = AccountStatus.CREATED;
            } else if (payload instanceof AccountEvent.Activated) {
                status = AccountStatus.ACTIVE;
            } else if (payload instanceof AccountEvent.Credited event) {
                balance = balance.add(event.amount());
            } else if (payload instanceof AccountEvent.Debited event) {
                balance = balance.subtract(event.amount());
            } else if (payload instanceof AccountEvent.StatusChanged event) {
                status = event.status();
            }
            sequence = message.getSequenceNumber();
            seen = true;
        }
        if (!seen) {
            throw new AccountNotFoundException(query.accountId());
        }
        return new StateSnapshot(query.accountId(), balance, currency, status, sequence);
    }
}
