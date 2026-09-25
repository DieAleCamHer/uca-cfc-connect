package edu.udb.ucacfc.inscripcion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    List<Inscripcion> findByClienteId(Long clienteId);

    List<Inscripcion> findByCursoId(Long cursoId);

    List<Inscripcion> findByDiplomadoId(Long diplomadoId);

    List<Inscripcion> findByEstado(EstadoInscripcion estado);

    // Util para el Service: saber cuantos inscritos activos tiene un curso
    // antes de validar el cupo maximo.
    long countByCursoIdAndEstadoIn(Long cursoId, List<EstadoInscripcion> estados);

    long countByDiplomadoIdAndEstadoIn(Long diplomadoId, List<EstadoInscripcion> estados);
}
