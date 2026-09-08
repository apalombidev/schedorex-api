package com.apalombidev.schedorex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
    
    @Bean
    public SecurityFilterChain temporaryPermissiveFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}