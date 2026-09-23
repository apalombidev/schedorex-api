package com.apalombidev.schedorex.i18n;

import com.apalombidev.schedorex.service.ServiceSetupService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * Imposta la Locale corrente su LocaleContextHolder in base a
 * ProfessionalService.language: la lingua della piattaforma è
 * un'impostazione di configurazione per istanza (decisione esplicita), non
 * dipende dal client.
 *
 * Non è annotato @Component: viene registrato esplicitamente con ordine
 * alto da LocaleFilterConfig, per garantire che giri PRIMA della catena di
 * Spring Security — altrimenti RestAuthenticationEntryPoint/
 * RestAccessDeniedHandler (401/403, Step 8) userebbero ancora la Locale di
 * default, e la Bean Validation su @Valid (che scatta durante il dispatch,
 * dopo i filtri servlet) non troverebbe la Locale già impostata.
 *
 * try/finally con resetLocaleContext(): necessario perché in un thread pool
 * (Tomcat/Undertow) il thread viene riusato tra richieste diverse — senza
 * reset, la Locale di una richiesta potrebbe "trapelare" sulla successiva
 * se per qualche motivo questo filtro non venisse rieseguito.
 */
public class LocaleFilter extends OncePerRequestFilter {

    private final ServiceSetupService serviceSetupService;

    public LocaleFilter(ServiceSetupService serviceSetupService) {
        this.serviceSetupService = serviceSetupService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        Locale locale = serviceSetupService.getCurrentLocale();
        LocaleContextHolder.setLocale(locale);
        try {
            filterChain.doFilter(request, response);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}