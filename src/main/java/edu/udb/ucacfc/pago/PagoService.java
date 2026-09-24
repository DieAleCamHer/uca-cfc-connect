package edu.udb.ucacfc.pago;

import java.math.BigDecimal;
import java.util.List;

public interface PagoService {

    /**
     * Crea el registro de pago pendiente para una referencia (inscripcion,
     * cotizacion, alquiler o catering) con el monto total a pagar.
     */
    Pago crear(TipoReferenciaPago tipoReferencia, Long referenciaId, BigDecimal monto, MetodoPago metodo);

    /**
     * Aplica un abono/pago sobre un registro existente y recalcula el estado
     * (PENDIENTE / PARCIAL / PAGADO) segun cuanto se ha pagado vs el monto total.
     */
    Pago abonar(Long pagoId, BigDecimal montoAbonado);

    Pago buscarPorId(Long id);

    List<Pago> listarPorReferencia(TipoReferenciaPago tipoReferencia, Long referenciaId);

    List<Pago> listarPorEstado(EstadoPago estado);
}
