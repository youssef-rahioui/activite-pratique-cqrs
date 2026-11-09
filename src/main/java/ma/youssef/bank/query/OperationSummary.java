package ma.youssef.bank.query;

import java.math.BigDecimal;
import java.time.Instant;

public record OperationSummary(String id, String accountId, BigDecimal amount, String currency, String type, Instant occurredAt) {
    public static OperationSummary from(AccountOperation operation) {
        return new OperationSummary(operation.getId(), operation.getAccountId(), operation.getAmount(),
                operation.getCurrency(), operation.getType(), operation.getOccurredAt());
    }
}
