package edu.udb.ucacfc.seguridad.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank(message = "El refreshToken es obligatorio") String refreshToken) {
}
