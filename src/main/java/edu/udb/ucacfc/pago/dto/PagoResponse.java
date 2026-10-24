package edu.udb.ucacfc.pago.dto;

import edu.udb.ucacfc.pago.EstadoPago;
import edu.udb.ucacfc.pago.Pago;
import edu.udb.ucacfc.pago.TipoReferenciaPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** saldo = total - pagado (validado). enValidacion = transferencias/depositos reportados aun sin validar. */
public record PagoResponse(Long id, TipoReferenciaPago tipoReferencia, Long referenciaId, Long clienteId,
                           String clienteNombre, String concepto, BigDecimal montoTotal, BigDecimal montoPagado,
                           BigDecimal saldo, BigDecimal enValidacion, EstadoPago estado, LocalDateTime fechaGeneracion,
                           LocalDateTime fechaPagoCompleto, String motivoAnulacion, List<AbonoResponse> abonos) {

    public static PagoResponse de(Pago p, BigDecimal enValidacion, List<AbonoResponse> abonos) {
        return new PagoResponse(p.getId(), p.getTipoReferencia(), p.getReferenciaId(), p.getCliente().getId(),
                p.getCliente().nombreMostrado(), p.getConcepto(), p.getMontoTotal(), p.getMontoPagado(), p.saldo(),
                enValidacion, p.getEstado(), p.getFechaCreacion(), p.getFechaPagoCompleto(), p.getMotivoAnulacion(),
                abonos);
    }
}
