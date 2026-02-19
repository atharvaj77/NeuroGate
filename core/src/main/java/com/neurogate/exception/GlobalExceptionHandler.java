package com.neurogate.exception;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AllProvidersFailedException.class)
    public ResponseEntity<ErrorResponse> handleAllProvidersFailed(AllProvidersFailedException exception) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException exception) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(exception.getRetryAfterSeconds()));
        return new ResponseEntity<>(
                toErrorResponse(exception, HttpStatus.TOO_MANY_REQUESTS, exception.getMessage()),
                headers,
                HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRequestNotPermitted(RequestNotPermitted exception) {
        ErrorResponse response = new ErrorResponse(
                "rate_limit_exceeded",
                NeuroGateException.ErrorCode.RATE_LIMIT_EXCEEDED.getCode(),
                "Rate limit exceeded. Please retry later.",
                traceId(),
                Instant.now(),
                Map.of("source", exception.getMessage()));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(NeuroGateException.class)
    public ResponseEntity<ErrorResponse> handleNeuroGateException(NeuroGateException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request payload.");

        ErrorResponse response = new ErrorResponse(
                "invalid_request",
                NeuroGateException.ErrorCode.INVALID_REQUEST.getCode(),
                message,
                traceId(),
                Instant.now(),
                Map.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception exception) {
        log.error("Unhandled exception", exception);
        ErrorResponse response = new ErrorResponse(
                "internal_error",
                NeuroGateException.ErrorCode.INTERNAL_ERROR.getCode(),
                "An unexpected error occurred.",
                traceId(),
                Instant.now(),
                Map.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, NeuroGateException exception) {
        return ResponseEntity.status(status).body(toErrorResponse(exception, status, exception.getMessage()));
    }

    private ErrorResponse toErrorResponse(NeuroGateException exception, HttpStatus status, String message) {
        return new ErrorResponse(
                exception.getErrorCode().name().toLowerCase(),
                exception.getErrorCode().getCode(),
                message,
                traceId(),
                Instant.now(),
                Map.of("status", String.valueOf(status.value())));
    }

    private String traceId() {
        return MDC.get("traceId");
    }

    public record ErrorResponse(
            String errorCode,
            int code,
            String message,
            String traceId,
            Instant timestamp,
            Map<String, String> metadata
    ) {}
}
