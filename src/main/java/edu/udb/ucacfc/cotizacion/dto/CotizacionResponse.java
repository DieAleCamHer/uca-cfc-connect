package edu.udb.ucacfc.cotizacion.dto;

import edu.udb.ucacfc.cotizacion.EstadoCotizacion;
import edu.udb.ucacfc.cotizacion.TipoCotizacion;
import edu.udb.ucacfc.pago.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record CotizacionResponse(Long id, Long clienteId, String clienteNombre, TipoCotizacion tipo,
                                 String descripcion, LocalDate fechaEvento, LocalDateTime fechaSolicitud,
                                 LocalDate fechaVencimiento, List<ItemCotizacionResponse> items, BigDecimal subtotal,
                                 BigDecimal descuento, BigDecimal total, EstadoCotizacion estado, String motivoRechazo,
                                 String elaboradaPor, String aprobadaPor, LocalDateTime fechaDecision, Long pagoId,
                                 EstadoPago estadoPago, BigDecimal saldo, Set<String> accionesPosibles) {
}
