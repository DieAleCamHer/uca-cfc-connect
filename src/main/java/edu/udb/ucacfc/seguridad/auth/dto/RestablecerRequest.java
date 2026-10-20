package edu.udb.ucacfc.seguridad.auth.dto;

import edu.udb.ucacfc.shared.util.Patrones;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RestablecerRequest(
        @NotBlank(message = "El token es obligatorio") String token,
        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Pattern(regexp = Patrones.PASSWORD, message = Patrones.MSG_PASSWORD) String nuevaPassword) {
}
