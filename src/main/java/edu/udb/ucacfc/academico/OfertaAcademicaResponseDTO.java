package edu.udb.ucacfc.academico;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record OfertaAcademicaResponseDTO(
        Long id,
        String nombre,
        Long categoriaId,
        String categoriaNombre,
        Long modalidadId,
        String modalidadNombre,
        Long docenteId,
        String docenteNombre,
        Long espacioId,
        String espacioNombre,
        Integer cupoMaximo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        @JsonFormat(pattern = "HH:mm:ss") @Schema(type = "string", pattern = "HH:mm:ss", example = "18:00:00") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm:ss") @Schema(type = "string", pattern = "HH:mm:ss", example = "20:00:00") LocalTime horaFin,
        BigDecimal costo,
        boolean activo
) {
    public static OfertaAcademicaResponseDTO desde(Curso c) {
        return new OfertaAcademicaResponseDTO(c.getId(), c.getNombre(),
                c.getCategoria().getId(), c.getCategoria().getNombre(),
                c.getModalidad().getId(), c.getModalidad().getNombre(),
                c.getDocente().getId(), c.getDocente().getNombre(),
                c.getEspacio() != null ? c.getEspacio().getId() : null,
                c.getEspacio() != null ? c.getEspacio().getNombre() : null,
                c.getCupoMaximo(), c.getFechaInicio(), c.getFechaFin(),
                c.getHoraInicio(), c.getHoraFin(), c.getCosto(), c.isActivo());
    }

    public static OfertaAcademicaResponseDTO desde(Diplomado d) {
        return new OfertaAcademicaResponseDTO(d.getId(), d.getNombre(),
                d.getCategoria().getId(), d.getCategoria().getNombre(),
                d.getModalidad().getId(), d.getModalidad().getNombre(),
                d.getDocente().getId(), d.getDocente().getNombre(),
                d.getEspacio() != null ? d.getEspacio().getId() : null,
                d.getEspacio() != null ? d.getEspacio().getNombre() : null,
                d.getCupoMaximo(), d.getFechaInicio(), d.getFechaFin(),
                d.getHoraInicio(), d.getHoraFin(), d.getCosto(), d.isActivo());
    }
}
