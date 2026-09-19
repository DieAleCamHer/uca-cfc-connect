package edu.udb.ucacfc.espacio;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservaEspacioRequestDTO(
        @NotNull(message = "El espacio es obligatorio") Long espacioId,
        @NotNull(message = "El cliente es obligatorio") Long clienteId,
        @NotNull(message = "La fecha/hora de inicio es obligatoria") LocalDateTime fechaHoraInicio,
        @NotNull(message = "La fecha/hora de fin es obligatoria") LocalDateTime fechaHoraFin,
        String motivo
) {
}
