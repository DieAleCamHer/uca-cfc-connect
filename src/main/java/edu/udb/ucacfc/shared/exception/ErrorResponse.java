package edu.udb.ucacfc.shared.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Forma estandar de TODOS los errores que devuelve la API.
 * Los controllers nunca construyen esto a mano: lo arma el GlobalExceptionHandler.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> detalles
) {
    public ErrorResponse(int status, String error, String message) {
        this(LocalDateTime.now(), status, error, message, null);
    }

    public ErrorResponse(int status, String error, String message, List<String> detalles) {
        this(LocalDateTime.now(), status, error, message, detalles);
    }
}
