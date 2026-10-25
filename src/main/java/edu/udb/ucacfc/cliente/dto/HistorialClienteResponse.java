package edu.udb.ucacfc.cliente.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** RF14 Consultar historial de clientes: resumen por modulo, finanzas y movimientos recientes. */
public record HistorialClienteResponse(
        ClienteResponse cliente,
        Map<String, Long> inscripciones,
        Map<String, Long> reservas,
        Map<String, Long> catering,
        Map<String, Long> cotizaciones,
        BigDecimal totalFacturado,
        BigDecimal totalPagado,
        BigDecimal saldoPendiente,
        List<Movimiento> recientes) {

    public record Movimiento(String modulo, Long id, String descripcion, LocalDateTime fecha, String estado,
                             BigDecimal monto) {
    }
}
