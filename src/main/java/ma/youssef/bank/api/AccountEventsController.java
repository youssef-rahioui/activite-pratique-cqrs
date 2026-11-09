package ma.youssef.bank.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import ma.youssef.bank.query.AccountNotFoundException;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountEventsController {
    private final EventStore events;

    public AccountEventsController(EventStore events) {
        this.events = events;
    }

    public record RecordedEvent(long sequence, String type, Instant occurredAt, Object payload) {}

    @GetMapping("/{id}/events")
    public List<RecordedEvent> events(@PathVariable String id) {
        DomainEventStream stream = events.readEvents(id);
        List<RecordedEvent> history = new ArrayList<>();
        while (stream.hasNext()) {
            DomainEventMessage<?> event = stream.next();
            history.add(new RecordedEvent(event.getSequenceNumber(), event.getPayloadType().getSimpleName(),
                    event.getTimestamp(), event.getPayload()));
        }
        if (history.isEmpty()) {
            throw new AccountNotFoundException(id);
        }
        return history;
    }
}
