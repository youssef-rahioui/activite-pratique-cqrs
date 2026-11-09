package ma.youssef.bank.query;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class AccountQueryHandler {
    private final AccountRepository accounts;
    private final OperationRepository operations;

    public AccountQueryHandler(AccountRepository accounts, OperationRepository operations) {
        this.accounts = accounts;
        this.operations = operations;
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
}
