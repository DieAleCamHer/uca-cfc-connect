package edu.udb.ucacfc.seguridad.usuario.dto;

import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.seguridad.usuario.Usuario;

import java.time.LocalDateTime;

/** Datos publicos de una cuenta: nunca incluye password, tokenVersion ni el token de recuperacion. */
public record UsuarioResponse(Long id, String nombre, String email, Rol rol, boolean activo,
                              LocalDateTime ultimoAcceso, LocalDateTime fechaCreacion, Long clienteId) {

    public static UsuarioResponse de(Usuario u, Long clienteId) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.isActivo(),
                u.getUltimoAcceso(), u.getFechaCreacion(), clienteId);
    }
}
