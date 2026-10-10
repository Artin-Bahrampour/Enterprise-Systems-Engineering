package dev.portfolio.treasury.api;

import dev.portfolio.treasury.service.DomainExceptions;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DomainExceptions.ResourceNotFound.class)
    ProblemDetail notFound(DomainExceptions.ResourceNotFound ex) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(DomainExceptions.IdempotencyConflict.class)
    ProblemDetail conflict(DomainExceptions.IdempotencyConflict ex) {
        return problem(HttpStatus.CONFLICT, "Idempotency conflict", ex.getMessage());
    }

    @ExceptionHandler(DomainExceptions.InsufficientFunds.class)
    ProblemDetail insufficientFunds(DomainExceptions.InsufficientFunds ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "Data integrity conflict",
                "The request conflicts with an existing record or database constraint.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail result = ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
        result.setTitle(title);
        result.setType(URI.create("https://example.invalid/problems/" + status.value()));
        return result;
    }
}
