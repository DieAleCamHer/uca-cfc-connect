package edu.udb.ucacfc.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Se ejecuta UNA vez por cada request (OncePerRequestFilter). Si viene un
 * header "Authorization: Bearer <token>" valido, autentica al usuario en
 * el SecurityContext para que el resto del pipeline (y los Controllers)
 * sepan quien esta haciendo la peticion y con que rol.
 *
 * Si no viene token, o es invalido, simplemente deja pasar la peticion sin
 * autenticar: seran las reglas de SecurityConfig las que decidan si esa
 * ruta requiere autenticacion o no.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsServiceImpl usuarioDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioDetailsServiceImpl usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(PREFIJO_BEARER.length());
            String email = jwtService.extraerUsername(token);

            boolean nadieAutenticadoTodavia = SecurityContextHolder.getContext().getAuthentication() == null;
            if (email != null && nadieAutenticadoTodavia) {
                UserDetails userDetails = usuarioDetailsService.loadUserByUsername(email);
                if (jwtService.esValido(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            // Token malformado, expirado, o con firma invalida: no se autentica.
            // JwtAuthenticationEntryPoint se encarga de responder 401 mas adelante
            // si la ruta requeria autenticacion.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
