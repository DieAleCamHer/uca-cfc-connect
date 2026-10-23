package edu.udb.ucacfc.inscripcion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

/**
 * Los metodos ...AndIdNot reciben -1 al crear (no excluye nada) y el id propio al editar.
 */
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long>, JpaSpecificationExecutor<Inscripcion> {

    long countByCursoIdAndEstadoIn(Long cursoId, Collection<EstadoInscripcion> estados);

    long countByDiplomadoIdAndEstadoIn(Long diplomadoId, Collection<EstadoInscripcion> estados);

    boolean existsByCursoIdAndClienteIdAndParticipanteDuiIsNullAndEstadoInAndIdNot(
            Long cursoId, Long clienteId, Collection<EstadoInscripcion> estados, Long id);

    boolean existsByDiplomadoIdAndClienteIdAndParticipanteDuiIsNullAndEstadoInAndIdNot(
            Long diplomadoId, Long clienteId, Collection<EstadoInscripcion> estados, Long id);

    boolean existsByCursoIdAndParticipanteDuiAndEstadoInAndIdNot(
            Long cursoId, String dui, Collection<EstadoInscripcion> estados, Long id);

    boolean existsByDiplomadoIdAndParticipanteDuiAndEstadoInAndIdNot(
            Long diplomadoId, String dui, Collection<EstadoInscripcion> estados, Long id);

    List<Inscripcion> findByCursoIdAndEstadoIn(Long cursoId, Collection<EstadoInscripcion> estados);

    List<Inscripcion> findByDiplomadoIdAndEstadoIn(Long diplomadoId, Collection<EstadoInscripcion> estados);

    List<Inscripcion> findByEstado(EstadoInscripcion estado);

    long countByClienteIdAndEstadoIn(Long clienteId, Collection<EstadoInscripcion> estados);

    long countByEstado(EstadoInscripcion estado);

    long countByClienteIdAndEstado(Long clienteId, EstadoInscripcion estado);

    List<Inscripcion> findTop10ByClienteIdOrderByIdDesc(Long clienteId);
}
