package com.todo.eod.infra.conf;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(name = "eod.security.enabled", havingValue = "false", matchIfMissing = true)
public class NoAuthSecurityConfig {

    @Bean
    public SecurityFilterChain noAuthFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/**").permitAll()
            );
        // Sem Resource Server, sem sessão, sem nada — tudo liberado.
        return http.build();
    }
}
