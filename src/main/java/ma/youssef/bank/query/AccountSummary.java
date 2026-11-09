package ma.youssef.bank.query;

import java.math.BigDecimal;
import java.time.Instant;
import ma.youssef.bank.domain.AccountStatus;

public record AccountSummary(String id, BigDecimal balance, String currency, AccountStatus status, Instant createdAt) {
    public static AccountSummary from(AccountView account) {
        return new AccountSummary(account.getId(), account.getBalance(), account.getCurrency(),
                account.getStatus(), account.getCreatedAt());
    }
}
