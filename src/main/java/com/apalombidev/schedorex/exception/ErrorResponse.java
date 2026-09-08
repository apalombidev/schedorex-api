package com.apalombidev.schedorex.exception;

import java.time.Instant;
import java.util.List;

/**
 * Corpo di risposta uniforme per tutti gli errori gestiti da
 * GlobalExceptionHandler. fieldErrors è null per gli errori "semplici"
 * (dominio, 404/409/401/500), valorizzato solo dalle violazioni Bean
 * Validation (400 su @Valid).
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(Instant.now(), status, error, message, null);
    }

    public static ErrorResponse ofValidation(int status, String error, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, fieldErrors);
    }
}