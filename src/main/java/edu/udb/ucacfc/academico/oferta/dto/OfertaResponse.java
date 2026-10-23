package edu.udb.ucacfc.academico.oferta.dto;

import edu.udb.ucacfc.academico.oferta.EstadoOferta;
import edu.udb.ucacfc.academico.oferta.TipoOferta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OfertaResponse(Long id, TipoOferta tipo, String nombre, String descripcion,
                             Long categoriaId, String categoriaNombre,
                             Long modalidadId, String modalidadNombre, boolean requiereEspacioFisico,
                             Long docenteId, String docenteNombre,
                             Long espacioId, String espacioNombre,
                             int cupoMaximo, long inscritos, long cupoDisponible, BigDecimal costo,
                             LocalDateTime inicio, LocalDateTime fin, double horasTotales,
                             EstadoOferta estado, boolean activo, List<SesionResponse> sesiones) {
}
