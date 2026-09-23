package com.apalombidev.schedorex.service.dto;

/**
 * Sollevata dal compact constructor di ServiceSetupRequest (Step 4) quando
 * timezone non è un identificatore IANA valido. Estende
 * IllegalArgumentException per restare compatibile con il meccanismo già
 * verificato in v8/v9 (Jackson incapsula l'eccezione del costruttore del
 * record dentro HttpMessageNotReadableException), ma — a differenza della
 * prima stesura, che costruiva il messaggio finale in italiano qui dentro —
 * porta il valore originale come campo esplicito. Il record non ha (e non
 * deve avere) accesso a MessageSource: la traduzione avviene solo in
 * GlobalExceptionHandler.
 */
public class InvalidTimezoneException extends IllegalArgumentException {

    private final String timezone;

    public InvalidTimezoneException(String timezone) {
        super("error.timezone.invalid"); // solo per log/stacktrace, mai esposto al client
        this.timezone = timezone;
    }

    public String getTimezone() {
        return timezone;
    }
}