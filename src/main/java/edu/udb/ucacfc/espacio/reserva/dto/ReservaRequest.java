package edu.udb.ucacfc.espacio.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ReservaRequest(
        @Schema(description = "Obligatorio para el personal; un CLIENTE siempre reserva a su nombre (se ignora)", example = "1")
        @Positive Long clienteId,
        @Schema(example = "1") @NotNull(message = "El espacio es obligatorio") @Positive Long espacioId,
        @Schema(example = "2026-11-20T08:00") @NotNull(message = "El inicio es obligatorio") LocalDateTime inicio,
        @Schema(example = "2026-11-20T12:00") @NotNull(message = "El fin es obligatorio") LocalDateTime fin,
        @Schema(example = "80") @NotNull(message = "El numero de asistentes es obligatorio")
        @Positive(message = "El numero de asistentes debe ser mayor a cero") Integer numeroAsistentes,
        @Schema(example = "Capacitacion anual del personal") @NotBlank(message = "Indica el proposito del evento")
        @Size(max = 200) String proposito) {
}
