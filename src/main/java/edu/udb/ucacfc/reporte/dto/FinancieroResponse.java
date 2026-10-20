package edu.udb.ucacfc.reporte.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * facturado: cuentas generadas en el rango (sin anuladas). recaudado: abonos VALIDADOS en el rango.
 * pendienteDeCobro: saldo actual de todas las cuentas abiertas (no depende del rango).
 */
public record FinancieroResponse(LocalDate desde, LocalDate hasta, BigDecimal facturado, BigDecimal recaudado,
                                 BigDecimal pendienteDeCobro, BigDecimal enValidacion, long abonosPorValidar,
                                 Map<String, BigDecimal> recaudadoPorMetodo,
                                 Map<String, BigDecimal> facturadoPorTipo, Map<String, Long> cuentasPorEstado) {
}
