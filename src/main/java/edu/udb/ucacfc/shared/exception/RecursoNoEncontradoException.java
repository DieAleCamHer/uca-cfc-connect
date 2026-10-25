package edu.udb.ucacfc.shared.exception;

import org.springframework.http.HttpStatus;

/** 404: se busco algo por id y no existe. */
public class RecursoNoEncontradoException extends ApiException {
    public RecursoNoEncontradoException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static RecursoNoEncontradoException de(String entidad, Object id) {
        return new RecursoNoEncontradoException(entidad + " con id " + id + " no existe");
    }
}
