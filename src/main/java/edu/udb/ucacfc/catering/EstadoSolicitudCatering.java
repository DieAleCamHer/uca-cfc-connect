package edu.udb.ucacfc.catering;

import java.util.EnumSet;
import java.util.Set;

/**
 * PENDIENTE  -> pedido recibido (si usa un espacio libre, ya lo bloquea).
 * CONFIRMADA -> el personal acepta atenderlo; se genera la cuenta (precio x asistentes).
 * ATENDIDA   -> el evento ocurrio Y esta pagado (automatico, o manual desde que inicia). Final.
 * RECHAZADA  -> el personal no puede atenderlo (con motivo). Final.
 * CANCELADA  -> cancelado por el cliente o el personal antes del evento (o vencido sin confirmar). Final.
 */
public enum EstadoSolicitudCatering {
    PENDIENTE, CONFIRMADA, ATENDIDA, RECHAZADA, CANCELADA;

    public static final Set<EstadoSolicitudCatering> VIGENTES = EnumSet.of(PENDIENTE, CONFIRMADA);
}
