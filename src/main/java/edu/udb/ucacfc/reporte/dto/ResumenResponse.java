package edu.udb.ucacfc.reporte.dto;

import java.util.List;
import java.util.Map;

public record ResumenResponse(long clientesActivos, long docentesActivos, long cursosActivos, long diplomadosActivos,
                              long espaciosActivos, Map<String, Long> inscripcionesPorEstado,
                              Map<String, Long> reservasPorEstado, Map<String, Long> cateringPorEstado,
                              Map<String, Long> cotizacionesPorEstado, List<OfertaTop> ofertasConMasInscritos) {

    public record OfertaTop(String tipo, Long id, String nombre, long inscritos, int cupoMaximo) {
    }
}
