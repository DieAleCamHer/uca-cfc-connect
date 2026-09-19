package edu.udb.ucacfc.shared.exception;

/**
 * Excepcion generica para cualquier regla de negocio que no encaje en las
 * mas especificas de arriba (ej: "fecha fin no puede ser antes que fecha
 * inicio", "modalidad virtual no puede reservar espacio fisico").
 * HTTP 400 Bad Request en el Bloque 4.
 */
public class OperacionInvalidaException extends RuntimeException {

    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
