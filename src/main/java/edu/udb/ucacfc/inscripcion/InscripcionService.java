package edu.udb.ucacfc.inscripcion;

import java.util.List;

public interface InscripcionService {

    /**
     * Inscribe a un cliente en un curso O en un diplomado (exactamente uno
     * de los dos ids debe venir con valor, el otro null).
     */
    Inscripcion inscribir(Long clienteId, Long cursoId, Long diplomadoId);

    Inscripcion buscarPorId(Long id);

    List<Inscripcion> listar();

    List<Inscripcion> listarPorCliente(Long clienteId);

    Inscripcion cambiarEstado(Long id, EstadoInscripcion nuevoEstado);
}
