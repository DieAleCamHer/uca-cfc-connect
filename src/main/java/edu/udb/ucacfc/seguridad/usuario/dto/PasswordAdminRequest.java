package edu.udb.ucacfc.seguridad.usuario.dto;

import edu.udb.ucacfc.shared.util.Patrones;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordAdminRequest(
        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Pattern(regexp = Patrones.PASSWORD, message = Patrones.MSG_PASSWORD) String nuevaPassword) {
}
