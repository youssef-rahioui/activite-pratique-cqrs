package ma.youssef.bank.api;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import ma.youssef.bank.domain.AccountCommand;
import ma.youssef.bank.domain.AccountStatus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountCommandsController {
    private final CommandGateway commands;

    public AccountCommandsController(CommandGateway commands) {
        this.commands = commands;
    }

    public record CreateRequest(@NotNull @PositiveOrZero BigDecimal initialBalance,
                                @NotNull @Pattern(regexp = "[A-Z]{3}") String currency) {}
    public record AmountRequest(@NotNull @Positive BigDecimal amount,
                                @NotNull @Pattern(regexp = "[A-Z]{3}") String currency) {}
    public record StatusRequest(@NotNull AccountStatus status) {}
    public record CommandResult(String accountId) {}

    @PostMapping
    public CompletableFuture<ResponseEntity<CommandResult>> create(@Valid @RequestBody CreateRequest request) {
        String id = UUID.randomUUID().toString();
        return commands.<String>send(new AccountCommand.Create(id, request.initialBalance(), request.currency()))
                .thenApply(result -> ResponseEntity.accepted().location(URI.create("/api/accounts/" + id))
                        .body(new CommandResult(id)));
    }

    @PostMapping("/{id}/credits")
    public CompletableFuture<ResponseEntity<CommandResult>> credit(@PathVariable String id,
                                                                     @Valid @RequestBody AmountRequest request) {
        return commands.<String>send(new AccountCommand.Credit(id, request.amount(), request.currency()))
                .thenApply(result -> ResponseEntity.accepted().body(new CommandResult(result)));
    }

    @PostMapping("/{id}/debits")
    public CompletableFuture<ResponseEntity<CommandResult>> debit(@PathVariable String id,
                                                                    @Valid @RequestBody AmountRequest request) {
        return commands.<String>send(new AccountCommand.Debit(id, request.amount(), request.currency()))
                .thenApply(result -> ResponseEntity.accepted().body(new CommandResult(result)));
    }

    @PatchMapping("/{id}/status")
    public CompletableFuture<ResponseEntity<CommandResult>> changeStatus(@PathVariable String id,
                                                                           @Valid @RequestBody StatusRequest request) {
        return commands.<String>send(new AccountCommand.ChangeStatus(id, request.status()))
                .thenApply(result -> ResponseEntity.accepted().body(new CommandResult(result)));
    }
}
