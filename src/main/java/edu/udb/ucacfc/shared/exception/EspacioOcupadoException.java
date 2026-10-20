package edu.udb.ucacfc.shared.exception;

import org.springframework.http.HttpStatus;

/** 409: el espacio ya tiene otra actividad que bloquea ese horario (regla "no conflictos de horario"). */
public class EspacioOcupadoException extends ApiException {
    public EspacioOcupadoException(String message) {
        super(HttpStatus.CONFLICT, "SPACE_OCCUPIED", message);
    }
}
