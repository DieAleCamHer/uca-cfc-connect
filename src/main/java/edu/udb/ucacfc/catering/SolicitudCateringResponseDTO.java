package edu.udb.ucacfc.catering;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

public record SolicitudCateringResponseDTO(
        Long id, Long clienteId, String clienteNombre, Long servicioId, String servicioNombre,
        Integer numeroAsistentes, String menu, LocalDate fecha,
        @JsonFormat(pattern = "HH:mm:ss") @Schema(type = "string", pattern = "HH:mm:ss", example = "09:00:00") LocalTime hora,
        String lugar,
        EstadoSolicitudCatering estado
) {
    public static SolicitudCateringResponseDTO desde(SolicitudCatering s) {
        return new SolicitudCateringResponseDTO(s.getId(),
                s.getCliente().getId(), s.getCliente().getNombre(),
                s.getServicio().getId(), s.getServicio().getNombre(),
                s.getNumeroAsistentes(), s.getMenu(), s.getFecha(), s.getHora(), s.getLugar(), s.getEstado());
    }
}
