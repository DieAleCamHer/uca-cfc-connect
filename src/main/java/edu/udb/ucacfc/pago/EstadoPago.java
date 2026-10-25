package edu.udb.ucacfc.pago;

/**
 * PENDIENTE: nada cobrado. PARCIAL: hay abonos validados pero falta saldo. PAGADO: saldado.
 * ANULADO (agregado para coherencia): el proceso se cancelo sin dinero cobrado; se conserva para auditoria
 * en vez de borrar la fila.
 */
public enum EstadoPago {
    PENDIENTE, PARCIAL, PAGADO, ANULADO
}
