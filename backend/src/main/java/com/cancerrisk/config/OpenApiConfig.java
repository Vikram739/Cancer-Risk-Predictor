package com.cancerrisk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cancerRiskOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cancer Risk Predictor API")
                        .version("1.0.0")
                        .description("Patients, ML backed risk assessments and dashboard statistics. "
                                + "For educational use only, not a diagnostic tool.")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
