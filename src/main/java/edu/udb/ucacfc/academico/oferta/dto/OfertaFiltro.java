package edu.udb.ucacfc.academico.oferta.dto;

import edu.udb.ucacfc.academico.oferta.EstadoOferta;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record OfertaFiltro(
        @Schema(description = "Texto en nombre o descripcion") String q,
        Long categoriaId,
        Long modalidadId,
        Long docenteId,
        Long espacioId,
        @Schema(description = "PROGRAMADA, EN_CURSO, FINALIZADA o INACTIVA") EstadoOferta estado,
        @Schema(description = "Ofertas que tienen actividad desde esta fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @Schema(description = "Ofertas que tienen actividad hasta esta fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @Schema(description = "true = solo con cupos disponibles") Boolean conCupo) {
}
