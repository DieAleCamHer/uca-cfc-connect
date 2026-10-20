package edu.udb.ucacfc.seguridad.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401 cuando una ruta protegida se pide SIN token (el filtro ya respondio si el token venia pero era malo). */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorWriter writer;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writer.escribir(request, response, HttpStatus.UNAUTHORIZED, "NO_TOKEN",
                "Debes iniciar sesion: envia el header Authorization: Bearer <accessToken>");
    }
}
