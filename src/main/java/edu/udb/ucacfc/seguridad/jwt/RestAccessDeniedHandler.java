package edu.udb.ucacfc.seguridad.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 cuando una regla de URL de SecurityConfig niega el acceso (antes de MVC).
 * Los 403 de @PreAuthorize los responde GlobalExceptionHandler y los de dueno del registro, los services.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter writer;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        writer.escribir(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "Tu rol no tiene permiso para realizar esta accion");
    }
}
