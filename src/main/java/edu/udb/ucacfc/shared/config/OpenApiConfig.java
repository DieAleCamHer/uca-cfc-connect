package edu.udb.ucacfc.shared.config;

import edu.udb.ucacfc.shared.exception.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Swagger / OpenAPI.
 *  - @SecurityScheme "bearerAuth": hace aparecer el boton Authorize (candado). Se pega SOLO el token.
 *  - @OpenAPIDefinition(security = bearerAuth): candado en todos los endpoints; los publicos lo quitan con
 *    @SecurityRequirements() vacio (AuthController, PublicoController).
 *  - respuestasDeError(): agrega a cada operacion las respuestas de error estandar con el esquema ErrorResponse,
 *    para no repetir @ApiResponses en 120 metodos.
 * Esto es SOLO documentacion: quien permite o bloquea de verdad es SecurityConfig + @PreAuthorize.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "UCA-CFC Connect API",
                version = "2.0",
                description = """
                        Sistema del **Centro de Formacion Continua de la UCA** (DWF404 - Proyecto de catedra).

                        **Como probar endpoints protegidos (candado):**
                        1. `POST /api/auth/login` con `admin@ucacfc.edu.sv` / `Admin12345` (o un usuario de ejemplo del README).
                        2. Copia el valor de `accessToken`.
                        3. Pulsa **Authorize** y pega **solo el token** (Swagger agrega `Bearer `).

                        **Convenciones**
                        - Listados: `page` (desde 0), `size` (1-100), `sort=campo,asc|desc` (varios con `;`). Cada endpoint indica los campos ordenables.
                        - Fechas `AAAA-MM-DD`, fecha-hora `AAAA-MM-DDTHH:mm`, horas `HH:mm`. Zona horaria America/El_Salvador.
                        - `DELETE` en catalogos = desactivar (borrado logico). En procesos (inscripciones, reservas, catering, cotizaciones) = cancelar.
                        - Errores: siempre `ErrorResponse` con `code` (NOT_FOUND, DUPLICATE, BUSINESS_RULE, STATE_CONFLICT, INVALID_TRANSITION, SPACE_OCCUPIED, TEACHER_BUSY, CAPACITY_EXCEEDED, FORBIDDEN_OPERATION, ACCESS_DENIED, TOKEN_EXPIRED...).
                        - Seguridad en 3 capas: token valido (filtro JWT) -> rol (@PreAuthorize) -> dueno del registro (services).
                        """,
                contact = @Contact(name = "DWF404 - Universidad Don Bosco")),
        servers = @Server(url = "/", description = "Mismo servidor"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Pega SOLO el accessToken devuelto por /api/auth/login (Swagger agrega 'Bearer ').")
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer respuestasDeError() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            Map<String, Schema> schemas = ModelConverters.getInstance().read(ErrorResponse.class);
            schemas.forEach((k, v) -> openApi.getComponents().addSchemas(k, v));
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((ruta, item) -> item.readOperationsMap().forEach((metodo, op) -> {
                boolean publica = op.getSecurity() != null && op.getSecurity().isEmpty();
                ApiResponses r = op.getResponses() == null ? new ApiResponses() : op.getResponses();
                agregar(r, "400", "Datos invalidos o regla de negocio incumplida");
                if (!publica) {
                    agregar(r, "401", "Sin token, token vencido, revocado o de tipo incorrecto");
                    agregar(r, "403", "Tu rol no puede usar este endpoint, o el registro pertenece a otro cliente");
                }
                if (ruta.contains("{")) {
                    agregar(r, "404", "El registro no existe");
                }
                if (metodo != PathItem.HttpMethod.GET) {
                    agregar(r, "409", "Choca con el estado actual: duplicado, horario ocupado, cupo lleno, transicion invalida...");
                }
                agregar(r, "500", "Error inesperado");
                op.setResponses(r);
            }));
        };
    }

    private void agregar(ApiResponses responses, String codigo, String descripcion) {
        if (responses.containsKey(codigo)) {
            return;
        }
        Schema<?> ref = new Schema<>().$ref("#/components/schemas/ErrorResponse");
        responses.addApiResponse(codigo, new ApiResponse().description(descripcion)
                .content(new Content().addMediaType("application/json", new MediaType().schema(ref))));
    }

}
