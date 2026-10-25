package edu.udb.ucacfc.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base de TODAS las excepciones de negocio. Lleva el codigo HTTP y un codigo corto (code) que el
 * frontend puede usar para decidir que hacer. GlobalExceptionHandler la traduce a ErrorResponse.
 * Es RuntimeException: un throw dentro de un metodo @Transactional revierte la transaccion completa.
 */
@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
