package edu.udb.ucacfc.shared.exception;

import org.springframework.http.HttpStatus;

/** 401 lanzado dentro de MVC (refresh invalido, token de recuperacion vencido, contrasena actual incorrecta). */
public class TokenInvalidoException extends ApiException {
    public TokenInvalidoException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
