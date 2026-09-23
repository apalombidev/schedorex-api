package com.apalombidev.schedorex.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Un solo MessageSource, usato sia da GlobalExceptionHandler (eccezioni di
 * dominio, errori tecnici come timezone/enum non validi) sia da Hibernate
 * Validator per i {key} nelle annotazioni Bean Validation dei DTO (Step 4)
 * — invece di un ValidationMessages.properties separato, per non mantenere
 * due cataloghi di traduzioni paralleli.
 *
 * Basename "i18n/messages": i file fisici sono
 * src/main/resources/i18n/messages_it.properties e messages_en.properties.
 * Nessun messages.properties "neutro": defaultLocale=ITALIAN forza IT come
 * fallback esplicito invece di dipendere dal default locale della JVM.
 */
@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("i18n/messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setDefaultLocale(Locale.ENGLISH);
        // Se una chiave manca in un file di traduzione, meglio vedere la
        // chiave stessa in risposta (facilmente riconoscibile e segnalabile)
        // che un'eccezione NoSuchMessageException che finirebbe nel fallback
        // generico 500 di GlobalExceptionHandler.
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }

    /**
     * ⚠️ Da verificare a runtime: setValidationMessageSource dovrebbe far sì
     * che Hibernate Validator, interpolando un template "{validation.xxx}",
     * consulti LocaleContextHolder.getLocale() automaticamente (comportamento
     * standard documentato di LocalValidatorFactoryBean) — non ancora
     * testato in questo progetto. Se il messaggio tornasse sempre in una
     * sola lingua indipendentemente da LocaleContextHolder, il primo posto
     * da controllare è qui.
     */
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}