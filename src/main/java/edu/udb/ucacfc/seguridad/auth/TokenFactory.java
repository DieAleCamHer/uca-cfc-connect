package edu.udb.ucacfc.seguridad.auth;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteRepository;
import edu.udb.ucacfc.seguridad.auth.dto.AuthResponse;
import edu.udb.ucacfc.seguridad.jwt.JwtService;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Un solo lugar que arma la respuesta con los dos tokens (login, registro, refresh, cambio de contrasena). */
@Component
@RequiredArgsConstructor
public class TokenFactory {

    private final JwtService jwtService;
    private final ClienteRepository clienteRepository;

    public AuthResponse emitir(Usuario u) {
        return armar(u, jwtService.generarRefresh(u));
    }

    /** Refresh: nuevo access, se reutiliza el mismo refresh hasta que venza. */
    public AuthResponse renovarAccess(Usuario u, String refreshActual) {
        return armar(u, refreshActual);
    }

    public UsuarioResponse perfil(Usuario u) {
        Long clienteId = clienteRepository.findByUsuarioId(u.getId()).map(Cliente::getId).orElse(null);
        return UsuarioResponse.de(u, clienteId);
    }

    private AuthResponse armar(Usuario u, String refresh) {
        return new AuthResponse(jwtService.generarAccess(u), refresh, "Bearer",
                jwtService.segundosAccess(), jwtService.segundosRefresh(), perfil(u));
    }
}
