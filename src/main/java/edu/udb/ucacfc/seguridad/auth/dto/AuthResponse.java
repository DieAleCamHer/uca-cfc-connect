package edu.udb.ucacfc.seguridad.auth.dto;

import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioResponse;

/** Respuesta de login, registro, refresh y cambio de contrasena. */
public record AuthResponse(String accessToken, String refreshToken, String tokenType,
                           long expiresIn, long refreshExpiresIn, UsuarioResponse usuario) {
}
