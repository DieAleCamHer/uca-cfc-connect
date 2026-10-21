package edu.udb.ucacfc.shared.web;

/** Respuesta simple para operaciones que solo confirman algo. */
public record MensajeResponse(String mensaje) {
    public static MensajeResponse of(String mensaje) {
        return new MensajeResponse(mensaje);
    }
}
