package edu.udb.ucacfc.shared.exception;

import org.springframework.http.HttpStatus;

/** 409: se intento un cambio de estado que la maquina de estados no permite. */
public class TransicionInvalidaException extends ApiException {
    public TransicionInvalidaException(String message) {
        super(HttpStatus.CONFLICT, "INVALID_TRANSITION", message);
    }
}
