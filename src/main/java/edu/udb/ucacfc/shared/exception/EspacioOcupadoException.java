package edu.udb.ucacfc.shared.exception;

/**
 * Se lanza cuando se intenta reservar/programar un espacio en un horario
 * que ya esta ocupado por otra actividad. Es la excepcion clave que pide
 * el lineamiento del proyecto (regla de "no cruces de horario").
 * En el Bloque 4 se traduce a HTTP 409 Conflict.
 */
public class EspacioOcupadoException extends RuntimeException {

    public EspacioOcupadoException(String mensaje) {
        super(mensaje);
    }
}
