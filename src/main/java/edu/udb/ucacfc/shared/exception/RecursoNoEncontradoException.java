package edu.udb.ucacfc.shared.exception;

/**
 * Se lanza cuando se busca un registro por id (o clave) y no existe.
 * En el Bloque 4 se traduce a HTTP 404 en el @ControllerAdvice.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException de(String entidad, Long id) {
        return new RecursoNoEncontradoException(entidad + " con id " + id + " no fue encontrado(a)");
    }
}
