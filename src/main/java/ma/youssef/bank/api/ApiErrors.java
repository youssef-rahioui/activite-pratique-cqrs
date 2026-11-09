package ma.youssef.bank.api;

import java.util.Map;
import ma.youssef.bank.query.AccountNotFoundException;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.axonframework.queryhandling.QueryExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalidRequest(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request body"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handle(Exception exception) {
        Throwable cause = exception;
        boolean commandFailure = false;
        boolean missingAccount = false;
        while (cause.getCause() != null && cause != cause.getCause()) {
            commandFailure |= cause instanceof CommandExecutionException;
            missingAccount |= cause instanceof AccountNotFoundException || cause instanceof AggregateNotFoundException
                    || (cause instanceof QueryExecutionException && cause.getMessage().contains("Account not found:"));
            cause = cause.getCause();
        }
        HttpStatus status = missingAccount || cause instanceof AccountNotFoundException || cause instanceof AggregateNotFoundException
                || (cause instanceof QueryExecutionException && cause.getMessage().contains("Account not found:"))
                ? HttpStatus.NOT_FOUND
                : cause instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST
                : commandFailure || cause instanceof IllegalStateException || cause instanceof CommandExecutionException
                ? HttpStatus.CONFLICT : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = status.is5xxServerError() ? "Internal server error" : cause.getMessage();
        return ResponseEntity.status(status).body(Map.of("error", message == null ? status.getReasonPhrase() : message));
    }
}
