package edu.udb.ucacfc.pago.dto;

import edu.udb.ucacfc.pago.EstadoPago;
import edu.udb.ucacfc.pago.MetodoPago;
import edu.udb.ucacfc.pago.TipoReferenciaPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Comprobante de pago (lineamiento Contabilidad: "Emitir comprobantes"). Solo existe para abonos VALIDADOS. */
public record ComprobanteResponse(String numero, LocalDateTime fechaEmision, String emisor, String clienteNombre,
                                  String clienteDocumento, String clienteCorreo, TipoReferenciaPago tipoReferencia,
                                  Long referenciaId, String concepto, BigDecimal montoAbonado, MetodoPago metodo,
                                  String referenciaExterna, BigDecimal totalCuenta, BigDecimal totalPagado,
                                  BigDecimal saldoPendiente, EstadoPago estadoCuenta, String registradoPor,
                                  String validadoPor) {
}
