package edu.udb.ucacfc.academico.oferta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Si la sesion termina de madrugada, fin es del dia siguiente (22:00 -> 01:00 del otro dia). */
public record SesionRequest(
        @Schema(example = "2026-11-03T18:00") @NotNull(message = "El inicio de la sesion es obligatorio") LocalDateTime inicio,
        @Schema(example = "2026-11-03T20:00") @NotNull(message = "El fin de la sesion es obligatorio") LocalDateTime fin) {
}
