package edu.udb.ucacfc.seguridad.usuario.dto;

import edu.udb.ucacfc.seguridad.usuario.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

public record UsuarioFiltro(
        @Schema(description = "Busca en nombre o correo") String q,
        Rol rol,
        Boolean activo) {
}
