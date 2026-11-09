package ma.youssef.bank.api;

import java.util.Map;
import ma.youssef.bank.query.AccountNotFoundException;
import org.axonframework.modelling.command.AggregateNotFoundException;
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
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }
        HttpStatus status = cause instanceof AccountNotFoundException || cause instanceof AggregateNotFoundException
                ? HttpStatus.NOT_FOUND
                : cause instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST
                : cause instanceof IllegalStateException ? HttpStatus.CONFLICT : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(Map.of("error", status.is5xxServerError() ? "Internal server error" : cause.getMessage()));
    }
}
