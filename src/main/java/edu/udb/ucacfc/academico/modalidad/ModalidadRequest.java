package edu.udb.ucacfc.academico.modalidad;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModalidadRequest(
        @Schema(example = "PRESENCIAL") @NotBlank(message = "El nombre es obligatorio") @Size(max = 50) String nombre,
        @Schema(example = "true") @NotNull(message = "Indica si la modalidad requiere espacio fisico")
        Boolean requiereEspacioFisico) {
}
