package com.clubimperio.gestionclub.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación de Swagger (solo activa con el perfil dev, ver application-dev.properties).
 * Declara el esquema "bearerAuth" para que Swagger UI muestre el botón "Authorize":
 * se pega el token del login una vez y Swagger lo manda en todas las requests.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Club Imperio API", version = "v1",
                description = "Gestión de socios, actividades, comisiones, inscripciones y asistencias."),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
