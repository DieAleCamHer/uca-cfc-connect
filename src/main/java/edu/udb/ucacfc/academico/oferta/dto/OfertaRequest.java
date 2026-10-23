package edu.udb.ucacfc.academico.oferta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Cuerpo compartido para crear/editar cursos y diplomados. */
public record OfertaRequest(
        @Schema(example = "Spring Boot para empresas") @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre admite maximo 150 caracteres") String nombre,
        @Schema(example = "APIs REST con seguridad JWT") @Size(max = 500) String descripcion,
        @Schema(example = "1") @NotNull(message = "La categoria es obligatoria") @Positive Long categoriaId,
        @Schema(example = "1") @NotNull(message = "La modalidad es obligatoria") @Positive Long modalidadId,
        @Schema(example = "1") @NotNull(message = "El docente es obligatorio") @Positive Long docenteId,
        @Schema(description = "Obligatorio si la modalidad requiere espacio fisico; prohibido si no", example = "1")
        @Positive Long espacioId,
        @Schema(example = "25") @NotNull(message = "El cupo maximo es obligatorio")
        @Positive(message = "El cupo maximo debe ser mayor a cero") @Max(value = 500, message = "El cupo maximo es 500")
        Integer cupoMaximo,
        @Schema(example = "150.00") @NotNull(message = "El costo es obligatorio")
        @DecimalMin(value = "0.00", message = "El costo no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo admite maximo 2 decimales") BigDecimal costo,
        @Schema(description = "Una o varias sesiones; no pueden cruzarse entre si")
        @NotEmpty(message = "Debe tener al menos una sesion") @Size(max = 200, message = "Maximo 200 sesiones")
        List<@Valid @NotNull SesionRequest> sesiones) {
}
