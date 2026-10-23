package edu.udb.ucacfc.inscripcion.dto;

import edu.udb.ucacfc.inscripcion.EstadoInscripcion;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record InscripcionFiltro(
        @Schema(description = "Ignorado para CLIENTE") Long clienteId,
        Long cursoId,
        Long diplomadoId,
        EstadoInscripcion estado,
        @Schema(description = "Texto en participante o cliente") String q,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
}
