package edu.udb.ucacfc.academico;

import jakarta.validation.constraints.NotBlank;

public record DocenteRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String especialidad,
        String correo,
        String telefono
) {
}
