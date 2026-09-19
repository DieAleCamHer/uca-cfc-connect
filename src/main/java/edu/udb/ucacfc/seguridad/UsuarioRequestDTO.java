package edu.udb.ucacfc.seguridad;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es valido") String email,
        @NotBlank(message = "La contrasena es obligatoria") @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres") String password,
        @NotNull(message = "El rol es obligatorio") Rol rol
) {
}
