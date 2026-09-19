package edu.udb.ucacfc.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.udb.ucacfc.shared.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Se dispara cuando alguien intenta acceder a una ruta protegida SIN
 * token (o con uno invalido) y la ruta lo requiere. Sin esto, Spring
 * Security devolveria una respuesta generica sin el formato JSON
 * consistente que usa el resto de la API (ErrorResponse).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                "Token invalido, expirado o no proporcionado");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
