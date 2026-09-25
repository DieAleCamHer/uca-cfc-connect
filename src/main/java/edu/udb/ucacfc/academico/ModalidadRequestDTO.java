package edu.udb.ucacfc.academico;

import jakarta.validation.constraints.NotBlank;

public record ModalidadRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        boolean requiereEspacioFisico
) {
}
