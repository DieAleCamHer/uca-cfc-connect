package edu.udb.ucacfc.seguridad.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * El mensaje siempre es el mismo exista o no la cuenta (no se revela quien esta registrado).
 * tokenDemo solo viene en modo academico (app.recuperacion.mostrar-token=true) porque no hay servidor de correo.
 */
public record RecuperacionResponse(
        String mensaje,
        @Schema(description = "Solo en modo academico: el token que llegaria por correo") String tokenDemo,
        Long expiraEnMinutos) {
}
