package edu.udb.ucacfc.agenda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventoRequest(
        @Schema(example = "Graduacion de diplomados 2026") @NotBlank(message = "El titulo es obligatorio") @Size(max = 150) String titulo,
        @Size(max = 500) String descripcion,
        @Schema(example = "2026-12-12T17:00") @NotNull(message = "El inicio es obligatorio") LocalDateTime inicio,
        @Schema(example = "2026-12-12T20:00") @NotNull(message = "El fin es obligatorio") LocalDateTime fin,
        @Schema(description = "Espacio del CFC (lo bloquea). Si es externo o virtual, usa lugarExterno") @Positive Long espacioId,
        @Size(max = 200) String lugarExterno,
        @Positive Integer asistentesEstimados,
        @Schema(example = "Coordinacion academica") @NotBlank(message = "Indica el responsable") @Size(max = 120) String responsable) {
}
