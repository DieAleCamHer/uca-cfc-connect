package edu.udb.ucacfc.pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponseDTO(
        Long id, TipoReferenciaPago tipoReferencia, Long referenciaId, BigDecimal monto,
        BigDecimal montoPagado, MetodoPago metodo, EstadoPago estado, LocalDateTime fechaPago
) {
    public static PagoResponseDTO desde(Pago p) {
        return new PagoResponseDTO(p.getId(), p.getTipoReferencia(), p.getReferenciaId(), p.getMonto(),
                p.getMontoPagado(), p.getMetodo(), p.getEstado(), p.getFechaPago());
    }
}
