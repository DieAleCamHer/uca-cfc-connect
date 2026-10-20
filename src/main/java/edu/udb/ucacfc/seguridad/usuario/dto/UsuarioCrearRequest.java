package edu.udb.ucacfc.seguridad.usuario.dto;

import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.shared.util.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioCrearRequest(
        @Schema(example = "Maria Recepcion") @NotBlank(message = "El nombre es obligatorio") @Size(max = 120)
        @Pattern(regexp = Patrones.NOMBRE_PERSONA, message = "El nombre solo puede contener letras, espacios, apostrofes, puntos y guiones")
        String nombre,
        @Schema(example = "recepcion2@ucacfc.edu.sv") @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido") @Size(max = 150) String email,
        @Schema(example = "Temporal2026") @NotBlank(message = "La contrasena es obligatoria")
        @Pattern(regexp = Patrones.PASSWORD, message = Patrones.MSG_PASSWORD) String password,
        @Schema(example = "RECEPCIONISTA", description = "ADMIN, RECEPCIONISTA o CONTABILIDAD")
        @NotNull(message = "El rol es obligatorio") Rol rol) {
}
