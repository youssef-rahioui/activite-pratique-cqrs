package ma.youssef.bank.domain;

import java.math.BigDecimal;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
public class AccountAggregate {
    @AggregateIdentifier
    private String accountId;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;

    protected AccountAggregate() {}

    @CommandHandler
    public AccountAggregate(AccountCommand.Create command) {
        if (command.initialBalance() == null || command.initialBalance().signum() < 0) {
            throw new IllegalArgumentException("Initial balance must be non-negative");
        }
        requireCurrency(command.currency());
        apply(new AccountEvent.Created(command.accountId(), command.initialBalance(), command.currency()));
        apply(new AccountEvent.Activated(command.accountId()));
    }

    @CommandHandler
    public String handle(AccountCommand.Credit command) {
        requireActive();
        requireAmountAndCurrency(command.amount(), command.currency());
        apply(new AccountEvent.Credited(accountId, command.amount(), currency));
        return accountId;
    }

    @CommandHandler
    public String handle(AccountCommand.Debit command) {
        requireActive();
        requireAmountAndCurrency(command.amount(), command.currency());
        if (balance.compareTo(command.amount()) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        apply(new AccountEvent.Debited(accountId, command.amount(), currency));
        return accountId;
    }

    @CommandHandler
    public String handle(AccountCommand.ChangeStatus command) {
        if (command.status() == null || command.status() == AccountStatus.CREATED) {
            throw new IllegalArgumentException("Status must be ACTIVE or SUSPENDED");
        }
        if (status == command.status()) {
            throw new IllegalStateException("Account already has this status");
        }
        apply(new AccountEvent.StatusChanged(accountId, command.status()));
        return accountId;
    }

    @EventSourcingHandler
    public void on(AccountEvent.Created event) {
        accountId = event.accountId();
        balance = event.initialBalance();
        currency = event.currency();
        status = AccountStatus.CREATED;
    }

    @EventSourcingHandler
    public void on(AccountEvent.Activated event) {
        status = AccountStatus.ACTIVE;
    }

    @EventSourcingHandler
    public void on(AccountEvent.Credited event) {
        balance = balance.add(event.amount());
    }

    @EventSourcingHandler
    public void on(AccountEvent.Debited event) {
        balance = balance.subtract(event.amount());
    }

    @EventSourcingHandler
    public void on(AccountEvent.StatusChanged event) {
        status = event.status();
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }
    }

    private void requireAmountAndCurrency(BigDecimal amount, String requestedCurrency) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!currency.equals(requestedCurrency)) {
            throw new IllegalArgumentException("Currency does not match the account");
        }
    }

    private static void requireCurrency(String currency) {
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a three-letter uppercase code");
        }
    }
}
