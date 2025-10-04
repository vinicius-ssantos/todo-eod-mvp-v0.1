package com.todo.eod.infra.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@ConditionalOnProperty(name = "eod.security.enabled", havingValue = "true")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/dod-policies/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/webhooks/github", "/webhooks/gitlab").permitAll()
                .requestMatchers(HttpMethod.POST, "/webhooks/observability", "/webhooks/flags").hasAuthority("SCOPE_webhooks:ingest")
                .requestMatchers("/flags/**", "/tasks/**").hasAuthority("SCOPE_tasks:*")
                .anyRequest().authenticated()
                .requestMatchers(HttpMethod.POST, "/webhooks/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt());
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${eod.security.jwt.secret:change-me}") String secret) {
        byte[] key = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}
