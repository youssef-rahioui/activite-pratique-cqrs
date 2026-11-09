package ma.youssef.bank.domain;

import java.math.BigDecimal;

public final class AccountEvent {
    private AccountEvent() {}

    public record Created(String accountId, BigDecimal initialBalance, String currency) {}

    public record Activated(String accountId) {}

    public record Credited(String accountId, BigDecimal amount, String currency) {}

    public record Debited(String accountId, BigDecimal amount, String currency) {}

    public record StatusChanged(String accountId, AccountStatus status) {}
}
