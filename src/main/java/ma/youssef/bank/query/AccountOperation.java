package ma.youssef.bank.query;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_operation", indexes = @Index(name = "account_operation_account_idx", columnList = "accountId"))
public class AccountOperation {
    @Id
    private String id;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String type;
    private Instant occurredAt;

    protected AccountOperation() {}

    public AccountOperation(String id, String accountId, BigDecimal amount, String currency, String type, Instant occurredAt) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.occurredAt = occurredAt;
    }

    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getType() { return type; }
    public Instant getOccurredAt() { return occurredAt; }
}
