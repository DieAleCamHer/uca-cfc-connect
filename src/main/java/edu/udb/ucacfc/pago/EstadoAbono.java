package edu.udb.ucacfc.pago;

/**
 * PENDIENTE_VALIDACION: transferencia/deposito reportado; aun NO cuenta como cobrado (pero reserva saldo).
 * VALIDADO: dinero confirmado; suma al montoPagado y tiene comprobante.
 * RECHAZADO: Contabilidad no encontro el dinero. ANULADO: un validado que se revirtio por error de registro.
 */
public enum EstadoAbono {
    PENDIENTE_VALIDACION, VALIDADO, RECHAZADO, ANULADO
}
