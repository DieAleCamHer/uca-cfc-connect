package edu.udb.ucacfc.espacio.dto;

import edu.udb.ucacfc.agenda.TipoActividad;
import edu.udb.ucacfc.espacio.TipoEspacio;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record DisponibilidadFiltro(
        @Schema(description = "Inicio (obligatorio)", example = "2026-11-10T08:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
        @Schema(description = "Fin (obligatorio)", example = "2026-11-10T12:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
        TipoEspacio tipo,
        Integer capacidadMinima,
        @Schema(description = "Al editar un registro: no contarlo como conflicto consigo mismo") TipoActividad excluirTipo,
        Long excluirReferenciaId) {
}
