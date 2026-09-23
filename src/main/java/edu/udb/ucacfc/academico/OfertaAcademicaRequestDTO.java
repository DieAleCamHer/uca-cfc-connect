package edu.udb.ucacfc.academico;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de entrada compartido entre Curso y Diplomado: tienen exactamente
 * los mismos campos (ver OfertaAcademica), asi que un solo record sirve
 * para los dos Controllers.
 */
public record OfertaAcademicaRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "La categoria es obligatoria") Long categoriaId,
        @NotNull(message = "La modalidad es obligatoria") Long modalidadId,
        @NotNull(message = "El docente es obligatorio") Long docenteId,
        // Obligatorio SOLO si la modalidad requiere espacio fisico (se valida
        // en el Service, no aqui, porque depende de un dato de otra tabla).
        Long espacioId,
        @NotNull @Positive(message = "El cupo maximo debe ser mayor a cero") Integer cupoMaximo,
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaFin,
        @NotNull
        @JsonFormat(pattern = "HH:mm:ss")
        @Schema(type = "string", pattern = "HH:mm:ss", example = "18:00:00")
        LocalTime horaInicio,
        @NotNull
        @JsonFormat(pattern = "HH:mm:ss")
        @Schema(type = "string", pattern = "HH:mm:ss", example = "20:00:00")
        LocalTime horaFin,
        @NotNull @DecimalMin(value = "0.0", message = "El costo no puede ser negativo") BigDecimal costo
) {
}
