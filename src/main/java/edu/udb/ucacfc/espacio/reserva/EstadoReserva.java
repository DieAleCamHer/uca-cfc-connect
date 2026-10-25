package edu.udb.ucacfc.espacio.reserva;

import java.util.EnumSet;
import java.util.Set;

/**
 * SOLICITADA -> pedida por el cliente o registrada por recepcion; YA bloquea el espacio en la agenda.
 * CONFIRMADA -> el personal la acepto; se genera la cuenta por cobrar (horas x precio por hora).
 * COMPLETADA -> el horario termino Y esta pagada (automatico o manual). Final.
 * RECHAZADA  -> el personal no la acepto (con motivo). Libera el espacio. Final.
 * CANCELADA  -> el cliente o el personal la cancelo antes de iniciar (o se vencio sin confirmar). Libera el espacio. Final.
 */
public enum EstadoReserva {
    SOLICITADA, CONFIRMADA, COMPLETADA, RECHAZADA, CANCELADA;

    public static final Set<EstadoReserva> VIGENTES = EnumSet.of(SOLICITADA, CONFIRMADA);
}
