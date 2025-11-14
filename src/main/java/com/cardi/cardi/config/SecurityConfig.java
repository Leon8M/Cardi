package com.cardi.cardi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity in this example, consider enabling for production
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/ws/**", "/app/**", "/topic/**").permitAll() // Permit all access to WebSocket endpoints
                .anyRequest().permitAll() // Permit all other requests (e.g., static files)
            );
        return http.build();
    }
}
