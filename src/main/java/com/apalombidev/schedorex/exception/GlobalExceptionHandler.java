package com.apalombidev.schedorex.exception;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.apalombidev.schedorex.auth.InvalidCredentialsException;
import com.apalombidev.schedorex.availability.InvalidAvailabilityRangeException;
import com.apalombidev.schedorex.availability.SlotNotFoundException;
import com.apalombidev.schedorex.booking.CancellationDeadlinePassedException;
import com.apalombidev.schedorex.booking.InvalidCancellationTokenException;
import com.apalombidev.schedorex.booking.SlotAlreadyBookedException;
import com.apalombidev.schedorex.booking.SlotBlockedException;
import com.apalombidev.schedorex.service.ServiceNotFoundException;

import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Gestione centralizzata delle eccezioni (v2, Step 6 — opzione B, già
 * scelta rispetto al try/catch sparso nei service). Traduce le eccezioni
 * di dominio dello Step 5 e le eccezioni "di framework" di deserializzazione
 * JSON/validazione in risposte HTTP coerenti, senza mai esporre uno stack
 * trace al client (v2 — "nessun codice errore tecnico esposto").
 *
 * Nota di ordine: con @ExceptionHandler puri (non estendendo
 * ResponseEntityExceptionHandler) Spring risolve il match in base alla
 * gerarchia delle eccezioni, non all'ordine dei metodi in questa classe —
 * non c'è quindi un problema di "mascheramento" tra gli handler qui sotto,
 * dato che le eccezioni gestite non condividono una gerarchia comune
 * (tranne il fallback Exception.class, sempre il meno specifico).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- 404 Not Found ----

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleServiceNotFound(ServiceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SlotNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSlotNotFound(SlotNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidCancellationTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCancellationToken(InvalidCancellationTokenException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ---- 409 Conflict ----

    @ExceptionHandler(SlotAlreadyBookedException.class)
    public ResponseEntity<ErrorResponse> handleSlotAlreadyBooked(SlotAlreadyBookedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Decisione presa qui (rimandata dallo Step 5, "409 o 400 da decidere"):
     * 409, non 400. Lo slot esiste ed è un riferimento sintatticamente
     * valido — il problema è lo stato corrente della risorsa (bloccata
     * dall'admin), non la richiesta del cliente in sé. Stessa famiglia
     * semantica di SlotAlreadyBookedException, messaggio distinto.
     */
    @ExceptionHandler(SlotBlockedException.class)
    public ResponseEntity<ErrorResponse> handleSlotBlocked(SlotBlockedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ---- 400 Bad Request ----

    @ExceptionHandler(CancellationDeadlinePassedException.class)
    public ResponseEntity<ErrorResponse> handleCancellationDeadlinePassed(CancellationDeadlinePassedException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidAvailabilityRangeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAvailabilityRange(InvalidAvailabilityRangeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Violazioni @Valid sui DTO (Step 4): clientEmail non valida,
     * durationMinutes negativo, activeDays vuoto, ecc. Elenco dei campi
     * falliti incluso nel body, utile al frontend per l'evidenziazione
     * del form (v7, promemoria Step 6).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ErrorResponse body = ErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Uno o più campi non sono validi.",
                fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Copre due casi distinti che Spring incapsula allo stesso modo (v4/v7/v8):
     * 1. Valore enum fuori dal set ammesso (ServiceCurrency, ServiceLanguage,
     *    BookingStatus) — Jackson fallisce con InvalidFormatException.
     * 2. timezone semanticamente non valido (ServiceSetupRequest, v8): il
     *    compact constructor del record lancia IllegalArgumentException,
     *    che Jackson incapsula (in genere in una ValueInstantiationException,
     *    a sua volta causa della HttpMessageNotReadableException).
     *
     * L'handler risale la catena delle cause per distinguere i due casi e
     * dare un messaggio più utile del generico "corpo non valido". Interviene
     * PRIMA di Bean Validation: un JSON con enum/timezone invalido fallisce
     * qui, il body non arriva mai a essere validato con @Valid — comportamento
     * da confermare con un test reale (v7 promemoria: "currency": "USD",
     * "timezone": "Europa/Roma").
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, extractReadableMessage(ex));
    }

    // ---- 401 Unauthorized ----

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // ---- 500 Internal Server Error (fallback) ----

    /**
     * Qualunque eccezione non gestita esplicitamente sopra. Messaggio
     * generico verso il client, stack trace loggato lato server (mai
     * serializzato nella risposta) — v2, "nessun codice errore tecnico
     * esposto".
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Errore non gestito", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Si è verificato un errore imprevisto. Riprova più tardi.");
    }

    // ---- Helper ----

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message));
    }

    private String extractReadableMessage(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
                String field = ife.getPath().get(ife.getPath().size() - 1).getPropertyName();
                return "Valore non valido per il campo '%s'.".formatted(field);
            }
            if (cause instanceof IllegalArgumentException iae && iae.getMessage() != null) {
                // Copre il messaggio già leggibile del compact constructor
                // di ServiceSetupRequest per il timezone (v8).
                return iae.getMessage();
            }
            cause = cause.getCause();
        }
        return "Corpo della richiesta non valido: verificare formato, enumerazioni (lingua, valuta, stato) e fuso orario.";
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Parametro '%s' non valido.".formatted(ex.getName());
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                null // fieldErrors non applicabile qui
        );
        return ResponseEntity.badRequest().body(body);
    }
}