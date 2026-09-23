package edu.udb.ucacfc.cotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CotizacionResponseDTO(
        Long id, Long clienteId, String clienteNombre, TipoCotizacion tipo, String descripcion,
        BigDecimal total, EstadoCotizacion estado, LocalDate fechaSolicitud
) {
    public static CotizacionResponseDTO desde(Cotizacion c) {
        return new CotizacionResponseDTO(c.getId(), c.getCliente().getId(), c.getCliente().getNombre(),
                c.getTipo(), c.getDescripcion(), c.getTotal(), c.getEstado(), c.getFechaSolicitud());
    }
}
