package edu.udb.ucacfc.seguridad.jwt;

import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.seguridad.usuario.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Convierte "Authorization: Bearer <token>" en un usuario autenticado.
 *  - Sin header: deja pasar ANONIMO; si la ruta exigia login, RestAuthenticationEntryPoint responde 401 NO_TOKEN.
 *  - Con header pero token malo/vencido/revocado/de tipo refresh, o cuenta desactivada: responde 401 aqui mismo
 *    con un code especifico, para que el frontend sepa si debe renovar (TOKEN_EXPIRED) o volver al login.
 *  - Token valido: relee el Usuario de la BD (rol y estado ACTUALES) y lo coloca en el SecurityContext.
 * Las rutas publicas de autenticacion se saltan el filtro (shouldNotFilter): un token viejo guardado en el
 * navegador nunca impide hacer login.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final SecurityErrorWriter writer;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.equals("/api/auth/login") || p.equals("/api/auth/registro") || p.equals("/api/auth/refresh")
                || p.equals("/api/auth/recuperar") || p.equals("/api/auth/restablecer")
                || p.startsWith("/api/publico/") || !p.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIJO)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(PREFIJO.length()).trim();

        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (ExpiredJwtException e) {
            rechazar(request, response, "TOKEN_EXPIRED",
                    "Tu sesion expiro. Renuevala con POST /api/auth/refresh o inicia sesion de nuevo");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            rechazar(request, response, "TOKEN_INVALID", "El token es invalido o fue alterado");
            return;
        }

        if (!jwtService.esAccess(claims)) {
            rechazar(request, response, "TOKEN_WRONG_TYPE",
                    "Enviaste un refresh token: en Authorization va el accessToken");
            return;
        }
        Optional<Usuario> encontrado = usuarioRepository.findByEmailIgnoreCase(claims.getSubject());
        if (encontrado.isEmpty()) {
            rechazar(request, response, "USER_NOT_FOUND", "La cuenta del token ya no existe");
            return;
        }
        Usuario usuario = encontrado.get();
        if (!usuario.isEnabled()) {
            rechazar(request, response, "ACCOUNT_DISABLED", "Tu cuenta esta desactivada");
            return;
        }
        if (!jwtService.coincideVersion(claims, usuario)) {
            rechazar(request, response, "TOKEN_REVOKED",
                    "Tu sesion fue cerrada (logout, cambio de contrasena o de rol). Inicia sesion de nuevo");
            return;
        }

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    private void rechazar(HttpServletRequest req, HttpServletResponse res, String code, String msg) throws IOException {
        SecurityContextHolder.clearContext();
        writer.escribir(req, res, HttpStatus.UNAUTHORIZED, code, msg);
    }
}
