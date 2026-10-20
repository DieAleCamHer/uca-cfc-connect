package edu.udb.ucacfc.shared.exception;

import org.springframework.http.HttpStatus;

/** 409: el docente ya imparte otra oferta cuyas sesiones se cruzan. */
public class DocenteOcupadoException extends ApiException {
    public DocenteOcupadoException(String message) {
        super(HttpStatus.CONFLICT, "TEACHER_BUSY", message);
    }
}
