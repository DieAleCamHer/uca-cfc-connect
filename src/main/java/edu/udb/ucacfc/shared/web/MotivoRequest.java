package edu.udb.ucacfc.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Cuerpo opcional para cancelar / rechazar con un motivo. */
public record MotivoRequest(
        @Schema(example = "El cliente cambio de fecha") @Size(max = 300, message = "El motivo admite maximo 300 caracteres")
        String motivo) {
}
