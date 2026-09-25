package edu.udb.ucacfc.catering;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record SolicitudCateringRequestDTO(
        @NotNull(message = "El cliente es obligatorio") Long clienteId,
        @NotNull(message = "El servicio es obligatorio") Long servicioId,
        @NotNull @Positive(message = "El numero de asistentes debe ser mayor a cero") Integer numeroAsistentes,
        String menu,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotNull(message = "La hora es obligatoria")
        @JsonFormat(pattern = "HH:mm:ss")
        @Schema(type = "string", pattern = "HH:mm:ss", example = "09:00:00")
        LocalTime hora,
        @NotBlank(message = "El lugar es obligatorio") String lugar
) {
}
