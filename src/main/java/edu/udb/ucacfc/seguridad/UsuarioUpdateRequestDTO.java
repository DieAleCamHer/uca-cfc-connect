package edu.udb.ucacfc.seguridad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO separado para PUT /api/usuarios/{id}. A proposito NO incluye email
 * ni password: cambiar el email o la contrasena son operaciones distintas
 * (y mas sensibles) que un update generico de nombre/rol, asi que no
 * deberian poder colarse sin querer en este endpoint.
 */
public record UsuarioUpdateRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "El rol es obligatorio") Rol rol
) {
}
