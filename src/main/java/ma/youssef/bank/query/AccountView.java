package ma.youssef.bank.query;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ma.youssef.bank.domain.AccountStatus;

@Entity
@Table(name = "account_view")
public class AccountView {
    @Id
    private String id;
    private BigDecimal balance;
    private String currency;
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    private Instant createdAt;

    protected AccountView() {}

    public AccountView(String id, BigDecimal balance, String currency, Instant createdAt) {
        this.id = id;
        this.balance = balance;
        this.currency = currency;
        this.createdAt = createdAt;
        this.status = AccountStatus.CREATED;
    }

    public String getId() { return id; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public AccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setStatus(AccountStatus status) { this.status = status; }
}
