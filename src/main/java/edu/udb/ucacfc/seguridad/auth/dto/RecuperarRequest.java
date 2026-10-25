package edu.udb.ucacfc.seguridad.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecuperarRequest(
        @Schema(example = "ana.perez@correo.com") @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido") String email) {
}
