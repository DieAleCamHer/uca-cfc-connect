package edu.udb.ucacfc.shared.exception;

/**
 * Se lanza cuando se intenta crear un registro que viola una restriccion
 * de unicidad de negocio (email repetido, DUI/NIT repetido, etc).
 * HTTP 409 Conflict en el Bloque 4.
 */
public class RegistroDuplicadoException extends RuntimeException {

    public RegistroDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
