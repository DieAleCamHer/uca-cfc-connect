package edu.udb.ucacfc.shared;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Sin esto, Swagger UI no sabe que la API usa JWT y NO muestra el boton
 * "Authorize" (candado). Con @SecurityScheme definido, aparece el candado;
 * con @SecurityRequirement a nivel global, TODOS los endpoints quedan
 * marcados como "requieren este esquema" en la documentacion (aunque los
 * que son publicos, como /api/auth/**, igual funcionan sin token: esto
 * solo afecta la documentacion visual, no el comportamiento real, que ya
 * lo controla SecurityConfig).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "UCA-CFC Connect API",
                version = "1.0",
                description = "API REST del Centro de Formación Continua de la UCA. " +
                        "Proyecto de cátedra DWF404 - Universidad Don Bosco."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Pega SOLO el token (sin la palabra 'Bearer', Swagger la agrega solo)."
)
public class OpenApiConfig {
}
