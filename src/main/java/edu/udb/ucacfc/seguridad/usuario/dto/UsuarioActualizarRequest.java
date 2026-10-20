package edu.udb.ucacfc.seguridad.usuario.dto;

import edu.udb.ucacfc.shared.util.Patrones;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos basicos. El rol, el estado y la contrasena tienen endpoints propios (son operaciones sensibles). */
public record UsuarioActualizarRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120)
        @Pattern(regexp = Patrones.NOMBRE_PERSONA, message = "El nombre solo puede contener letras, espacios, apostrofes, puntos y guiones")
        String nombre,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150) String email) {
}
