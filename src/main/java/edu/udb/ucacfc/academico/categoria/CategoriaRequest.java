package edu.udb.ucacfc.academico.categoria;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @Schema(example = "Tecnologias de la informacion") @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite maximo 100 caracteres") String nombre,
        @Schema(example = "Programacion, redes y ciberseguridad") @Size(max = 250) String descripcion) {
}
