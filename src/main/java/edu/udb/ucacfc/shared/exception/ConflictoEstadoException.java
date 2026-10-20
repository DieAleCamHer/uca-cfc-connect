package edu.udb.ucacfc.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * 409: la operacion es valida en si, pero choca con el ESTADO ACTUAL de los datos
 * (desactivar un docente con ofertas vigentes, cancelar algo con dinero cobrado, editar algo ya iniciado...).
 */
public class ConflictoEstadoException extends ApiException {
    public ConflictoEstadoException(String message) {
        super(HttpStatus.CONFLICT, "STATE_CONFLICT", message);
    }
}
