package ma.youssef.bank.query;

import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Component;
import ma.youssef.bank.domain.AccountEvent;
import ma.youssef.bank.domain.AccountStatus;

@Component
@ProcessingGroup("account-projection")
public class AccountProjection {
    private final AccountRepository accounts;
    private final OperationRepository operations;
    private final QueryUpdateEmitter updates;

    public AccountProjection(AccountRepository accounts, OperationRepository operations, QueryUpdateEmitter updates) {
        this.accounts = accounts;
        this.operations = operations;
        this.updates = updates;
    }

    @EventHandler
    public void on(AccountEvent.Created event, EventMessage<?> message) {
        accounts.save(new AccountView(event.accountId(), event.initialBalance(), event.currency(), message.getTimestamp()));
    }

    @EventHandler
    public void on(AccountEvent.Activated event) {
        changeStatus(event.accountId(), AccountStatus.ACTIVE);
    }

    @EventHandler
    public void on(AccountEvent.StatusChanged event) {
        changeStatus(event.accountId(), event.status());
    }

    @EventHandler
    public void on(AccountEvent.Credited event, EventMessage<?> message) {
        recordOperation(event.accountId(), event.amount(), event.currency(), "CREDIT", message, true);
    }

    @EventHandler
    public void on(AccountEvent.Debited event, EventMessage<?> message) {
        recordOperation(event.accountId(), event.amount(), event.currency(), "DEBIT", message, false);
    }

    private void changeStatus(String id, AccountStatus status) {
        AccountView account = accounts.findById(id).orElseThrow(() -> new IllegalStateException("Missing account projection: " + id));
        account.setStatus(status);
        accounts.save(account);
    }

    private void recordOperation(String id, java.math.BigDecimal amount, String currency, String type,
                                 EventMessage<?> message, boolean credit) {
        AccountView account = accounts.findById(id).orElseThrow(() -> new IllegalStateException("Missing account projection: " + id));
        AccountOperation operation = new AccountOperation(message.getIdentifier(), id, amount, currency, type, message.getTimestamp());
        account.setBalance(credit ? account.getBalance().add(amount) : account.getBalance().subtract(amount));
        accounts.save(account);
        operations.save(operation);
        updates.emit(AccountQuery.Watch.class, query -> query.accountId().equals(id), OperationSummary.from(operation));
    }
}
