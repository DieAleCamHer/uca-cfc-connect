package edu.udb.ucacfc.cliente.dto;

import edu.udb.ucacfc.shared.util.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Recepcion crea el acceso web de un cliente de mostrador. */
public record CrearCuentaRequest(
        @Schema(description = "Si se omite se usa el correo de la ficha", example = "carlos@delta.com.sv")
        @Email(message = "El correo no tiene un formato valido") String email,
        @Schema(example = "Temporal2026") @NotBlank(message = "La contrasena es obligatoria")
        @Pattern(regexp = Patrones.PASSWORD, message = Patrones.MSG_PASSWORD) String password) {
}
