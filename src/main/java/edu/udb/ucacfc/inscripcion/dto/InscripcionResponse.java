package edu.udb.ucacfc.inscripcion.dto;

import edu.udb.ucacfc.academico.oferta.TipoOferta;
import edu.udb.ucacfc.inscripcion.EstadoInscripcion;
import edu.udb.ucacfc.pago.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record InscripcionResponse(Long id, Long clienteId, String clienteNombre, TipoOferta tipoOferta, Long ofertaId,
                                  String ofertaNombre, LocalDateTime ofertaInicio, LocalDateTime ofertaFin,
                                  String participanteNombre, String participanteDui, String participanteCorreo,
                                  LocalDateTime fechaInscripcion, EstadoInscripcion estado, BigDecimal monto,
                                  Long pagoId, EstadoPago estadoPago, BigDecimal saldo,
                                  LocalDateTime fechaConfirmacion, LocalDateTime fechaCierre, String motivoCancelacion,
                                  @io.swagger.v3.oas.annotations.media.Schema(description = "Estados a los que TU rol puede moverla ahora")
                                  Set<EstadoInscripcion> accionesPosibles) {
}
