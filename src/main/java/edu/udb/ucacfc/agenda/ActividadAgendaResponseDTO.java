package edu.udb.ucacfc.agenda;

import java.time.LocalDateTime;

public record ActividadAgendaResponseDTO(
        Long id, TipoActividad tipo, Long referenciaId, String titulo,
        LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin,
        Long espacioId, String espacioNombre
) {
    public static ActividadAgendaResponseDTO desde(ActividadAgenda a) {
        return new ActividadAgendaResponseDTO(a.getId(), a.getTipo(), a.getReferenciaId(), a.getTitulo(),
                a.getFechaHoraInicio(), a.getFechaHoraFin(),
                a.getEspacio() != null ? a.getEspacio().getId() : null,
                a.getEspacio() != null ? a.getEspacio().getNombre() : null);
    }
}
