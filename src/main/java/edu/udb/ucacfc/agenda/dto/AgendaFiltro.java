package edu.udb.ucacfc.agenda.dto;

import edu.udb.ucacfc.agenda.TipoActividad;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AgendaFiltro(
        @Schema(description = "Fecha inicial (obligatoria)", example = "2026-11-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @Schema(description = "Fecha final inclusive (obligatoria, maximo 93 dias)", example = "2026-11-30")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        TipoActividad tipo,
        Long espacioId) {
}
