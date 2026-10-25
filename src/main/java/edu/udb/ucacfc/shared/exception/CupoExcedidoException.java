package edu.udb.ucacfc.shared.exception;

import org.springframework.http.HttpStatus;

/** 409: la oferta ya no tiene cupos. */
public class CupoExcedidoException extends ApiException {
    public CupoExcedidoException(String message) {
        super(HttpStatus.CONFLICT, "CAPACITY_EXCEEDED", message);
    }
}
