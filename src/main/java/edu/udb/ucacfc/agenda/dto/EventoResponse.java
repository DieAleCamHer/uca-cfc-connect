package edu.udb.ucacfc.agenda.dto;

import edu.udb.ucacfc.agenda.EstadoEvento;
import edu.udb.ucacfc.agenda.EventoInstitucional;

import java.time.LocalDateTime;

public record EventoResponse(Long id, String titulo, String descripcion, LocalDateTime inicio, LocalDateTime fin,
                             Long espacioId, String espacioNombre, String lugarExterno, Integer asistentesEstimados,
                             String responsable, EstadoEvento estado, String motivoCancelacion) {
    public static EventoResponse de(EventoInstitucional e) {
        return new EventoResponse(e.getId(), e.getTitulo(), e.getDescripcion(), e.getInicio(), e.getFin(),
                e.getEspacio() == null ? null : e.getEspacio().getId(),
                e.getEspacio() == null ? null : e.getEspacio().getNombre(), e.getLugarExterno(),
                e.getAsistentesEstimados(), e.getResponsable(), e.getEstado(), e.getMotivoCancelacion());
    }
}
