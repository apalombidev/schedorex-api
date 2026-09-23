package com.apalombidev.schedorex.exception;

/**
 * Classe base per tutte le eccezioni di dominio (Step 5). Prima di questa
 * feature, ogni eccezione portava un messaggio finale già in italiano,
 * costruito al momento del throw nel service layer. Ora porta una CHIAVE
 * di risoluzione (+ argomenti opzionali per MessageFormat) — il messaggio
 * localizzato viene costruito una sola volta, in GlobalExceptionHandler,
 * che è l'unico punto che conosce sia il MessageSource sia la Locale
 * corrente (LocaleContextHolder, impostata da LocaleFilter).
 *
 * getMessage() (ereditato da RuntimeException) resta la sola chiave, non il
 * testo tradotto: usato solo per i log applicativi, mai esposto al client.
 */
public abstract class DomainException extends RuntimeException {

    private final String messageKey;
    private final transient Object[] args;

    protected DomainException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}