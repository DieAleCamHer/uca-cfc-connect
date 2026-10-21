package edu.udb.ucacfc.agenda.dto;

import edu.udb.ucacfc.agenda.ActividadAgenda;
import edu.udb.ucacfc.agenda.TipoActividad;

import java.time.LocalDateTime;

public record ActividadResponse(Long id, TipoActividad tipo, Long referenciaId, String titulo, LocalDateTime inicio,
                                LocalDateTime fin, Long espacioId, String espacioNombre, boolean bloqueaEspacio,
                                String detalle) {
    public static ActividadResponse de(ActividadAgenda a) {
        return new ActividadResponse(a.getId(), a.getTipo(), a.getReferenciaId(), a.getTitulo(), a.getInicio(),
                a.getFin(), a.getEspacio() == null ? null : a.getEspacio().getId(),
                a.getEspacio() == null ? null : a.getEspacio().getNombre(), a.isBloqueaEspacio(), a.getDetalle());
    }
}
