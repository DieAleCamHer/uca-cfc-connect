package edu.udb.ucacfc.inscripcion;

import java.time.LocalDate;

public record InscripcionResponseDTO(
        Long id,
        Long clienteId,
        String clienteNombre,
        Long cursoId,
        String cursoNombre,
        Long diplomadoId,
        String diplomadoNombre,
        LocalDate fecha,
        EstadoInscripcion estado
) {
    public static InscripcionResponseDTO desde(Inscripcion i) {
        return new InscripcionResponseDTO(
                i.getId(),
                i.getCliente().getId(), i.getCliente().getNombre(),
                i.getCurso() != null ? i.getCurso().getId() : null,
                i.getCurso() != null ? i.getCurso().getNombre() : null,
                i.getDiplomado() != null ? i.getDiplomado().getId() : null,
                i.getDiplomado() != null ? i.getDiplomado().getNombre() : null,
                i.getFecha(), i.getEstado());
    }
}
