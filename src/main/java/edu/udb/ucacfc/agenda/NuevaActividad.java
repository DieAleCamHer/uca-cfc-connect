package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.espacio.Espacio;

import java.time.LocalDateTime;

/** Lo que un modulo le pide a la agenda que registre. */
public record NuevaActividad(TipoActividad tipo, Long referenciaId, String titulo, LocalDateTime inicio,
                             LocalDateTime fin, Espacio espacio, boolean bloqueaEspacio, Long clienteId,
                             String detalle) {

    public static NuevaActividad bloqueante(TipoActividad tipo, Long ref, String titulo, LocalDateTime inicio,
                                            LocalDateTime fin, Espacio espacio, Long clienteId) {
        return new NuevaActividad(tipo, ref, titulo, inicio, fin, espacio, true, clienteId, null);
    }
}
