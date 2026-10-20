package edu.udb.ucacfc.shared.exception;

import org.springframework.http.HttpStatus;

/** 409: se viola una unicidad de negocio (DUI, correo, nombre, inscripcion repetida...). */
public class RegistroDuplicadoException extends ApiException {
    public RegistroDuplicadoException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE", message);
    }
}
