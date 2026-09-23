package com.apalombidev.schedorex.exception;

import com.apalombidev.schedorex.service.dto.InvalidTimezoneException;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import tools.jackson.databind.exc.InvalidFormatException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    /**
     * Un solo handler per tutte le eccezioni di dominio (Step 5): la
     * risoluzione del messaggio (chiave + argomenti + Locale) è identica
     * per tutte, cambia solo lo status HTTP — vedi statusFor().
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        HttpStatus status = statusFor(ex);
        String message = resolveMessage(ex.getMessageKey(), ex.getArgs());
        return ResponseEntity.status(status).body(errorBody(status, message, null));
    }

    private HttpStatus statusFor(DomainException ex) {
        if (ex instanceof com.apalombidev.schedorex.service.ServiceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof com.apalombidev.schedorex.availability.SlotNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof com.apalombidev.schedorex.booking.InvalidCancellationTokenException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof com.apalombidev.schedorex.booking.SlotAlreadyBookedException) {
            return HttpStatus.CONFLICT;
        }
        if (ex instanceof com.apalombidev.schedorex.booking.SlotBlockedException) {
            return HttpStatus.CONFLICT;
        }
        if (ex instanceof com.apalombidev.schedorex.booking.CancellationDeadlinePassedException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof com.apalombidev.schedorex.availability.InvalidAvailabilityRangeException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof com.apalombidev.schedorex.auth.InvalidCredentialsException) {
            return HttpStatus.UNAUTHORIZED;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR; // non dovrebbe mai capitare: ogni sottoclasse è mappata sopra
    }

    /**
     * MethodArgumentNotValidException (Bean Validation su @Valid, Step 4/6):
     * i singoli messaggi di campo sono già tradotti da Hibernate Validator
     * (LocalValidatorFactoryBean + MessageConfig, questa feature) — qui
     * serve solo tradurre il messaggio "di cornice".
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        String message = resolveMessage("error.validation.failed", null);
        return ResponseEntity.badRequest().body(errorBody(HttpStatus.BAD_REQUEST, message, fieldErrors));
    }

    /**
     * HttpMessageNotReadableException: copre sia il timezone non valido
     * (InvalidTimezoneException, questa feature) sia un enum fuori dal set
     * ammesso (currency/language, Jackson 3.x InvalidFormatException, v9).
     * Cerca la causa risalendo l'intera catena invece di assumere un
     * livello di nesting fisso (findCause) — più robusto rispetto
     * all'assunzione originaria di v9/Step 4, che non specificava la
     * profondità esatta con cui Jackson incapsula l'eccezione del compact
     * constructor.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        InvalidTimezoneException tzEx = findCause(ex, InvalidTimezoneException.class);
        if (tzEx != null) {
            String message = resolveMessage("error.timezone.invalid", new Object[]{tzEx.getTimezone()});
            return ResponseEntity.badRequest().body(errorBody(HttpStatus.BAD_REQUEST, message, null));
        }

        InvalidFormatException fmtEx = findCause(ex, InvalidFormatException.class);
        if (fmtEx != null) {
            String property = fmtEx.getPath().isEmpty()
                    ? "?"
                    : fmtEx.getPath().get(fmtEx.getPath().size() - 1).getPropertyName();
            String message = resolveMessage("error.field.invalid_value", new Object[]{property, fmtEx.getValue()});
            return ResponseEntity.badRequest().body(errorBody(HttpStatus.BAD_REQUEST, message, null));
        }

        String message = resolveMessage("error.request.malformed", null);
        return ResponseEntity.badRequest().body(errorBody(HttpStatus.BAD_REQUEST, message, null));
    }

    /** Path variable non convertibile al tipo atteso (es. UUID malformato) — bug trovato allo Step 7. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = resolveMessage("error.param.invalid", new Object[]{ex.getName()});
        return ResponseEntity.badRequest().body(errorBody(HttpStatus.BAD_REQUEST, message, null));
    }

    /** Fallback generico — nessuno stack trace nel body, solo in log (invariato da v9). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Errore non gestito", ex);
        String message = resolveMessage("error.internal", null);
        return ResponseEntity.internalServerError()
                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, message, null));
    }

    private String resolveMessage(String key, Object[] args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    private ErrorResponse errorBody(HttpStatus status, String message, List<ErrorResponse.FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}