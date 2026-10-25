package edu.udb.ucacfc.seguridad.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.udb.ucacfc.shared.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Escribe un ErrorResponse JSON directamente en la respuesta. Lo usan los componentes que corren
 * ANTES de Spring MVC (filtro JWT, entry point, access denied handler), donde no existe @RestControllerAdvice.
 * Usa el ObjectMapper de Spring Boot para que las fechas salgan igual que en el resto de la API.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public void escribir(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String code,
                         String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getOutputStream(),
                ErrorResponse.of(status.value(), status.getReasonPhrase(), code, message, req.getRequestURI()));
    }
}
