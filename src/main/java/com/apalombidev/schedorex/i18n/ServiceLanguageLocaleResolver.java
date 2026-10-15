package com.apalombidev.schedorex.i18n;

import com.apalombidev.schedorex.service.ServiceSetupService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

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