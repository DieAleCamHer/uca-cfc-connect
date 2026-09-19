package edu.udb.ucacfc.seguridad;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es valido") String email,
        @NotBlank(message = "La contrasena es obligatoria") String password
) {
}
