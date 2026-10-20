package edu.udb.ucacfc.seguridad.auth.dto;

import edu.udb.ucacfc.shared.util.Patrones;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarPasswordRequest(
        @NotBlank(message = "La contrasena actual es obligatoria") String passwordActual,
        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Pattern(regexp = Patrones.PASSWORD, message = Patrones.MSG_PASSWORD) String nuevaPassword) {
}
