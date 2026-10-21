package edu.udb.ucacfc.catering.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lugar: reservaId (dentro de un alquiler del mismo cliente) O espacioId (espacio del CFC libre) O solo lugar (externo).
 */
public record SolicitudCateringRequest(
        @Schema(description = "Obligatorio para el personal; un CLIENTE siempre solicita a su nombre", example = "1")
        @Positive Long clienteId,
        @Schema(example = "1") @NotNull(message = "El servicio es obligatorio") @Positive Long servicioId,
        @Schema(example = "50") @NotNull(message = "El numero de asistentes es obligatorio")
        @Positive(message = "El numero de asistentes debe ser mayor a cero") @Max(value = 1000, message = "Maximo 1000 asistentes")
        Integer numeroAsistentes,
        @Schema(example = "Opcion vegetariana para 5 personas") @Size(max = 500) String menu,
        @Schema(example = "2026-11-20") @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @Schema(type = "string", example = "10:00") @JsonFormat(pattern = "HH:mm[:ss]")
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
        @Schema(type = "string", example = "10:30") @JsonFormat(pattern = "HH:mm[:ss]")
        @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin,
        @Schema(description = "Obligatorio si no hay reservaId ni espacioId (lugar externo)", example = "Hotel Sheraton, salon B")
        @Size(max = 200) String lugar,
        @Schema(description = "Espacio del CFC libre en ese horario (lo bloquea)") @Positive Long espacioId,
        @Schema(description = "Reserva vigente del mismo cliente dentro de la cual se sirve") @Positive Long reservaId) {
}
