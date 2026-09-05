package com.odc.plateforme_emploi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation interactive de l'API, accessible sur /swagger-ui.html une fois
 * l'application démarrée. Déclare le schéma d'authentification JWT pour que le
 * bouton "Authorize" de Swagger UI permette de tester directement les routes
 * protégées (coller le access token, sans le préfixe "Bearer ").
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_JWT = "bearerAuth";

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
            .info(new Info()
                .title("JobPlatform.GN — API")
                .description(
                    "API REST de la plateforme de mise en relation entre candidats et "
                    + "recruteurs en Guinée. Pour tester une route protégée : connectez-vous "
                    + "via /api/auth/login, copiez le champ \"token\" de la réponse, puis "
                    + "cliquez sur le bouton Authorize ci-dessous et collez-le."
                )
                .version("1.0.0")
                .contact(new Contact()
                    .name("JobPlatform.GN")
                    .email("no-reply@jobplatform.gn")))
            .addSecurityItem(new SecurityRequirement().addList(SCHEME_JWT))
            .components(new Components()
                .addSecuritySchemes(SCHEME_JWT, new SecurityScheme()
                    .name(SCHEME_JWT)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
