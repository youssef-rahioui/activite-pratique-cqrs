package ma.youssef.bank.domain;

import java.math.BigDecimal;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountAggregateTest {
    private FixtureConfiguration<AccountAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(AccountAggregate.class);
    }

    @Test
    void createsAndActivatesAccount() {
        fixture.givenNoPriorActivity()
                .when(new AccountCommand.Create("a-1", new BigDecimal("100.00"), "MAD"))
                .expectEvents(new AccountEvent.Created("a-1", new BigDecimal("100.00"), "MAD"),
                        new AccountEvent.Activated("a-1"));
    }

    @Test
    void rejectsOverdraftWithoutRecordingEvent() {
        fixture.given(new AccountEvent.Created("a-1", new BigDecimal("100.00"), "MAD"),
                        new AccountEvent.Activated("a-1"))
                .when(new AccountCommand.Debit("a-1", new BigDecimal("101.00"), "MAD"))
                .expectException(IllegalStateException.class)
                .expectNoEvents();
    }

    @Test
    void replaysCreditBeforeCheckingBalance() {
        fixture.given(new AccountEvent.Created("a-1", new BigDecimal("100.00"), "MAD"),
                        new AccountEvent.Activated("a-1"),
                        new AccountEvent.Credited("a-1", new BigDecimal("50.00"), "MAD"))
                .when(new AccountCommand.Debit("a-1", new BigDecimal("120.00"), "MAD"))
                .expectEvents(new AccountEvent.Debited("a-1", new BigDecimal("120.00"), "MAD"));
    }

    @Test
    void suspendedAccountCannotBeDebited() {
        fixture.given(new AccountEvent.Created("a-1", new BigDecimal("100.00"), "MAD"),
                        new AccountEvent.Activated("a-1"),
                        new AccountEvent.StatusChanged("a-1", AccountStatus.SUSPENDED))
                .when(new AccountCommand.Debit("a-1", BigDecimal.ONE, "MAD"))
                .expectException(IllegalStateException.class)
                .expectNoEvents();
    }

    @Test
    void rejectsCurrencyMismatch() {
        fixture.given(new AccountEvent.Created("a-1", BigDecimal.TEN, "MAD"),
                        new AccountEvent.Activated("a-1"))
                .when(new AccountCommand.Credit("a-1", BigDecimal.ONE, "EUR"))
                .expectException(IllegalArgumentException.class)
                .expectNoEvents();
    }
}
