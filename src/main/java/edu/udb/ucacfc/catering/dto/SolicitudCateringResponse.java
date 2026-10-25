package edu.udb.ucacfc.catering.dto;

import edu.udb.ucacfc.catering.EstadoSolicitudCatering;
import edu.udb.ucacfc.catering.TipoServicioCatering;
import edu.udb.ucacfc.pago.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record SolicitudCateringResponse(Long id, Long clienteId, String clienteNombre, Long servicioId,
                                        String servicioNombre, TipoServicioCatering tipoServicio, int numeroAsistentes,
                                        String menu, LocalDateTime inicio, LocalDateTime fin, String lugar,
                                        Long espacioId, String espacioNombre, Long reservaId,
                                        BigDecimal precioUnitario, BigDecimal total, EstadoSolicitudCatering estado,
                                        String motivoEstado, Long pagoId, EstadoPago estadoPago, BigDecimal saldo,
                                        Set<EstadoSolicitudCatering> accionesPosibles) {
}
