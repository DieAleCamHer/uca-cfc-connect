package edu.udb.ucacfc.shared.exception;

import org.springframework.http.HttpStatus;

/** 400: los datos no cumplen una regla de negocio (fechas incoherentes, modalidad virtual con aula, etc.). */
public class ReglaNegocioException extends ApiException {
    public ReglaNegocioException(String message) {
        super(HttpStatus.BAD_REQUEST, "BUSINESS_RULE", message);
    }
}
