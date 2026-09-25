package com.apalombidev.schedorex.i18n;

import com.apalombidev.schedorex.service.ServiceSetupService;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
@RequiredArgsConstructor
public class LocaleResolverConfig {

    private final ServiceSetupService serviceSetupService;

    @Bean
    public LocaleResolver localeResolver() {
        return new ServiceLanguageLocaleResolver(serviceSetupService);
    }
}
