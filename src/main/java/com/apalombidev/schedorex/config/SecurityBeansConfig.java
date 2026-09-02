package com.apalombidev.schedorex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bean minimi di sicurezza necessari già allo Step 5 (AuthService usa
 * PasswordEncoder per verificare la password in login). La configurazione
 * completa di Spring Security (SecurityFilterChain, filtro JWT, regole per
 * /api/admin/** vs /api/public/**) resta allo Step 8, come da piano (v2) —
 * questa classe verrà probabilmente assorbita in quella configurazione più
 * ampia a quel punto.
 */
@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}