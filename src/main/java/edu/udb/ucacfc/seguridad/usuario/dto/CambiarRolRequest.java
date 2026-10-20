package edu.udb.ucacfc.seguridad.usuario.dto;

import edu.udb.ucacfc.seguridad.usuario.Rol;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(@NotNull(message = "El rol es obligatorio") Rol rol) {
}
