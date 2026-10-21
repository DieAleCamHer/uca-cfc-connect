package edu.udb.ucacfc.espacio.reserva.dto;

import edu.udb.ucacfc.espacio.reserva.EstadoReserva;
import edu.udb.ucacfc.pago.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record ReservaResponse(Long id, Long espacioId, String espacioNombre, Long clienteId, String clienteNombre,
                              LocalDateTime inicio, LocalDateTime fin, int numeroAsistentes, String proposito,
                              BigDecimal horas, BigDecimal precioHora, BigDecimal total, EstadoReserva estado,
                              String motivoEstado, Long pagoId, EstadoPago estadoPago, BigDecimal saldo,
                              Set<EstadoReserva> accionesPosibles) {
}
