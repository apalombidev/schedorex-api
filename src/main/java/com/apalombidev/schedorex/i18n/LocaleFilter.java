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

public class LocaleFilter extends OncePerRequestFilter {

	private final ServiceSetupService serviceSetupService;

	public LocaleFilter(ServiceSetupService serviceSetupService) {
		this.serviceSetupService = serviceSetupService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Locale locale = serviceSetupService.getCurrentLocale();
		LocaleContextHolder.setLocale(locale);
		try {
			filterChain.doFilter(request, response);
		} finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}
}