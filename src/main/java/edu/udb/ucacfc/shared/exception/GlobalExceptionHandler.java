package edu.udb.ucacfc.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Centraliza el manejo de errores de TODOS los controllers. Cada
 * excepcion de negocio (definidas en este mismo paquete) se traduce
 * aqui a un codigo HTTP y un cuerpo JSON consistente (ErrorResponse).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EspacioOcupadoException.class)
    public ResponseEntity<ErrorResponse> handleEspacioOcupado(EspacioOcupadoException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CupoExcedidoException.class)
    public ResponseEntity<ErrorResponse> handleCupoExcedido(CupoExcedidoException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RegistroDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleDuplicado(RegistroDuplicadoException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OperacionInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleOperacionInvalida(OperacionInvalidaException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Login (Bloque 6): email o password incorrectos.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleCredencialesInvalidas(BadCredentialsException ex) {
        return construir(HttpStatus.UNAUTHORIZED, "Email o contrasena incorrectos");
    }

    // Login (Bloque 6): el usuario existe pero esta desactivado (activo=false).
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleUsuarioDesactivado(DisabledException ex) {
        return construir(HttpStatus.UNAUTHORIZED, "El usuario esta desactivado");
    }

    // Se dispara automaticamente cuando falla un @Valid sobre un DTO
    // (ej: @NotBlank, @Email, @Positive en los Request DTOs del Bloque 4).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", "Datos invalidos", detalles);
        return ResponseEntity.badRequest().body(body);
    }

    // Red de seguridad: cualquier otra excepcion no prevista no debe filtrar
    // detalles internos (stack traces) al cliente de la API.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerica(Exception ex) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado: " + ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
