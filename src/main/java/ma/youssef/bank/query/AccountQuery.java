package ma.youssef.bank.query;

public final class AccountQuery {
    private AccountQuery() {}

    public record All() {}
    public record ById(String accountId) {}
    public record Operations(String accountId) {}
    public record Watch(String accountId) {}
}
