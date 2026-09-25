package com.apalombidev.schedorex.i18n;

import com.apalombidev.schedorex.service.ServiceSetupService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * Sostituisce l'AcceptHeaderLocaleResolver di default di Spring MVC.
 *
 * BUG TROVATO IN VERIFICA: DispatcherServlet.processRequest() chiama SEMPRE
 * LocaleContextHolder.setLocaleContext(...) tramite il LocaleResolver
 * configurato, DOPO che LocaleFilter (un Filter servlet) ha già girato —
 * sovrascrivendo silenziosamente la Locale che avevamo impostato. Il
 * LocaleResolver di default (AcceptHeaderLocaleResolver) senza header
 * Accept-Language ricade sulla Locale del server/JVM: il test "funzionava"
 * con language=EN solo perché coincideva per caso con quella di sistema.
 *
 * LocaleFilter resta comunque necessario per i 401/403 di Spring Security,
 * che girano nella security chain prima che DispatcherServlet (e quindi
 * questo LocaleResolver) vengano mai raggiunti.
 */
public class ServiceLanguageLocaleResolver implements LocaleResolver {

    private final ServiceSetupService serviceSetupService;

    public ServiceLanguageLocaleResolver(ServiceSetupService serviceSetupService) {
        this.serviceSetupService = serviceSetupService;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return serviceSetupService.getCurrentLocale();
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException(
                "La lingua è derivata da ProfessionalService.language, non è impostabile per singola richiesta.");
    }
}