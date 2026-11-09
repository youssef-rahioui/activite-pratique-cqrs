package ma.youssef.bank.domain;

import java.math.BigDecimal;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public final class AccountCommand {
    private AccountCommand() {}

    public record Create(String accountId, BigDecimal initialBalance, String currency) {}

    public record Credit(@TargetAggregateIdentifier String accountId, BigDecimal amount, String currency) {}

    public record Debit(@TargetAggregateIdentifier String accountId, BigDecimal amount, String currency) {}

    public record ChangeStatus(@TargetAggregateIdentifier String accountId, AccountStatus status) {}
}
