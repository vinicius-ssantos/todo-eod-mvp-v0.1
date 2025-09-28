package com.todo.eod.infra.conf;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("TODO EoD API").version("0.1.0").description("Evidence-of-Done Todo API (MVP)"))
                .externalDocs(new ExternalDocumentation().description("README").url("https://example.local/README"));
    }
}
