package edu.udb.ucacfc.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo para rechazos donde el motivo es obligatorio (se le comunica al cliente). */
public record MotivoObligatorioRequest(
        @Schema(example = "No hay disponibilidad de personal para esa fecha")
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 300, message = "El motivo admite maximo 300 caracteres")
        String motivo) {
}
