package edu.udb.ucacfc.academico;

import jakarta.validation.constraints.NotBlank;

public record CategoriaRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String descripcion
) {
}
