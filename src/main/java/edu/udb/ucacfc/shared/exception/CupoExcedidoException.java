package edu.udb.ucacfc.shared.exception;

/**
 * Se lanza cuando se intenta inscribir a un cliente en un curso/diplomado
 * que ya alcanzo su cupo maximo. HTTP 409 Conflict en el Bloque 4.
 */
public class CupoExcedidoException extends RuntimeException {

    public CupoExcedidoException(String mensaje) {
        super(mensaje);
    }
}
