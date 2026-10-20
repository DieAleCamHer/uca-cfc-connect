package edu.udb.ucacfc.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * 403 de DUENO DEL REGISTRO: el rol si puede usar el endpoint (lo dejo pasar @PreAuthorize),
 * pero el registro pertenece a otro cliente.
 */
public class AccesoDenegadoException extends ApiException {
    public AccesoDenegadoException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN_OPERATION", message);
    }
}
