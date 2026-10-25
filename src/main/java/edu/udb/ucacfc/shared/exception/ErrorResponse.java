package edu.udb.ucacfc.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Molde UNICO de todo error de la API: lo arman GlobalExceptionHandler (errores dentro de MVC) y
 * RestAuthenticationEntryPoint / RestAccessDeniedHandler / JwtAuthenticationFilter (errores de seguridad).
 */
@Schema(description = "Formato estandar de error")
public record ErrorResponse(
        @Schema(example = "2026-10-17T10:15:30") LocalDateTime timestamp,
        @Schema(example = "409") int status,
        @Schema(example = "Conflict") String error,
        @Schema(example = "SPACE_OCCUPIED", description = "Codigo corto para el frontend") String code,
        @Schema(example = "El espacio 'Auditorio A' ya esta ocupado entre ...") String message,
        @Schema(example = "/api/reservas") String path,
        @Schema(description = "Solo en errores de validacion: campo: mensaje") List<String> details) {

    public static ErrorResponse of(int status, String error, String code, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, code, message, path, null);
    }

    public static ErrorResponse of(int status, String error, String code, String message, String path,
                                   List<String> details) {
        return new ErrorResponse(LocalDateTime.now(), status, error, code, message, path, details);
    }
}
