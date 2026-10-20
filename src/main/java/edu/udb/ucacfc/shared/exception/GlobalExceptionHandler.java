package edu.udb.ucacfc.shared.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Traduce CUALQUIER excepcion lanzada dentro de Spring MVC (controllers, services, @PreAuthorize)
 * a un ErrorResponse JSON uniforme. Los controllers no tienen try/catch.
 *
 * Errores que nacen ANTES de MVC (sin token, token vencido, rol insuficiente por URL) no llegan aqui:
 * los responden JwtAuthenticationFilter, RestAuthenticationEntryPoint y RestAccessDeniedHandler con el mismo molde.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Todas nuestras excepciones de negocio (404, 409, 400, 403 de dueno, 401 de refresh...). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), req, null);
    }

    /** @Valid sobre un @RequestBody o un @ParameterObject: lista campo -> mensaje. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::campo)
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        ex.getBindingResult().getGlobalErrors()
                .forEach(g -> details.add(g.getObjectName() + ": " + g.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos invalidos", req, details);
    }

    /** Filtros/paginacion (@ParameterObject) con un valor que no se puede convertir, ej. ?page=abc o ?estado=XYZ. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest req) {
        List<String> details = ex.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": valor invalido '" + fe.getRejectedValue() + "'")
                .toList();
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Parametros de consulta invalidos", req, details);
    }

    /**
     * Spring 6.1: si el metodo tiene restricciones en @PathVariable/@RequestParam (ej. @Positive en el id), la
     * validacion se hace "a nivel de metodo" y TAMBIEN los errores del @Valid @RequestBody llegan aqui.
     * Por eso se distingue: error de un campo del body -> "campo: mensaje"; error de un parametro -> "param: mensaje".
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<String> details = ex.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> e instanceof FieldError fe ? campo(fe)
                                : r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos invalidos", req, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage()).toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos invalidos", req, details);
    }

    /** JSON roto, fecha mal escrita, enum inexistente o campo que no existe en el DTO. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String msg = "El cuerpo de la peticion no se pudo leer: revisa que sea JSON valido y que fechas (AAAA-MM-DD), "
                + "fechas-hora (AAAA-MM-DDTHH:mm) y horas (HH:mm) tengan el formato correcto.";
        Throwable causa = ex.getCause();
        if (causa instanceof UnrecognizedPropertyException upe) {
            msg = "El campo '" + upe.getPropertyName() + "' no es aceptado en esta operacion. Campos validos: "
                    + upe.getKnownPropertyIds();
        } else if (causa instanceof InvalidFormatException ife && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            msg = "Valor invalido '" + ife.getValue() + "'. Valores permitidos: "
                    + Arrays.toString(ife.getTargetType().getEnumConstants());
        }
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", msg, req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Falta el parametro obligatorio '" + ex.getParameterName() + "'", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String msg = "Valor invalido para '" + ex.getName() + "': " + ex.getValue();
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            msg += ". Valores permitidos: " + Arrays.toString(ex.getRequiredType().getEnumConstants());
        }
        return build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", msg, req, null);
    }

    /** sort por un campo que no existe en la entidad (segunda red; PaginacionUtil ya valida la lista blanca). */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleSort(PropertyReferenceException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_SORT",
                "No se puede ordenar por '" + ex.getPropertyName() + "'", req, null);
    }

    /** @PreAuthorize negado (AuthorizationDeniedException hereda de AccessDeniedException). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "Tu rol no tiene permiso para realizar esta accion", req, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Correo o contrasena incorrectos", req, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED",
                "Tu cuenta esta desactivada. Comunicate con el Centro de Formacion Continua.", req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "El metodo " + ex.getMethod() + " no esta permitido en esta ruta", req, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMedia(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Envia el cuerpo como application/json", req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND", "La ruta solicitada no existe", req, null);
    }

    /** Ultima red para unicidades de la BD que se saltaran las validaciones del Service. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violacion de integridad en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY",
                "La operacion viola una restriccion de datos (registro duplicado o referencia invalida)", req, null);
    }

    /** Dos usuarios modificaron el mismo registro a la vez (@Version) o un bloqueo de fila expiro. */
    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleConcurrency(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Otro usuario modifico este registro al mismo tiempo. Recarga y vuelve a intentarlo.", req, null);
    }

    /** Red de seguridad: se registra en el log y NO se filtra el detalle tecnico al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrio un error inesperado. Si persiste, contacta al administrador.", req, null);
    }

    private String campo(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                HttpServletRequest req, List<String> details) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), code, message,
                        req.getRequestURI(), details));
    }
}
