package edu.udb.ucacfc.espacio;

import java.time.LocalDateTime;

public record ReservaEspacioResponseDTO(
        Long id, Long espacioId, String espacioNombre, Long clienteId, String clienteNombre,
        LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, String motivo, EstadoReserva estado
) {
    public static ReservaEspacioResponseDTO desde(ReservaEspacio r) {
        return new ReservaEspacioResponseDTO(r.getId(),
                r.getEspacio().getId(), r.getEspacio().getNombre(),
                r.getCliente().getId(), r.getCliente().getNombre(),
                r.getFechaHoraInicio(), r.getFechaHoraFin(), r.getMotivo(), r.getEstado());
    }
}
