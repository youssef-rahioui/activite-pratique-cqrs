package ma.youssef.bank.query;

import java.math.BigDecimal;
import ma.youssef.bank.domain.AccountStatus;

public record StateSnapshot(String accountId, BigDecimal balance, String currency, AccountStatus status, long sequence) {}
