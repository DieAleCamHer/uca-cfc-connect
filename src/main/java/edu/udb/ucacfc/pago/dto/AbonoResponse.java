package edu.udb.ucacfc.pago.dto;

import edu.udb.ucacfc.pago.Abono;
import edu.udb.ucacfc.pago.EstadoAbono;
import edu.udb.ucacfc.pago.MetodoPago;
import edu.udb.ucacfc.seguridad.usuario.Rol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AbonoResponse(Long id, Long pagoId, String concepto, String clienteNombre, BigDecimal monto,
                            MetodoPago metodo, String referenciaExterna, EstadoAbono estado, String registradoPor,
                            Rol rolRegistro, LocalDateTime fechaRegistro, String validadoPor,
                            LocalDateTime fechaValidacion, String motivo, String numeroComprobante) {
    public static AbonoResponse de(Abono a) {
        return new AbonoResponse(a.getId(), a.getPago().getId(), a.getPago().getConcepto(),
                a.getPago().getCliente().nombreMostrado(), a.getMonto(), a.getMetodo(), a.getReferenciaExterna(),
                a.getEstado(), a.getRegistradoPor(), a.getRolRegistro(), a.getFechaRegistro(), a.getValidadoPor(),
                a.getFechaValidacion(), a.getMotivo(), a.getNumeroComprobante());
    }
}
