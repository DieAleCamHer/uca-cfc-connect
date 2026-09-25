package edu.udb.ucacfc.inscripcion;

import jakarta.validation.constraints.NotNull;

public record InscripcionRequestDTO(
        @NotNull(message = "El cliente es obligatorio") Long clienteId,
        Long cursoId,
        Long diplomadoId
) {
}
