package edu.udb.ucacfc.academico.oferta;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Consultas comunes de Curso y Diplomado (#{#entityName} se reemplaza por Curso o Diplomado). */
@NoRepositoryBean
public interface OfertaRepository<T extends OfertaAcademica> extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

    /** SELECT ... FOR UPDATE: serializa inscripciones simultaneas para que el cupo nunca se exceda. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from #{#entityName} o where o.id = :id")
    Optional<T> bloquear(@Param("id") Long id);

    /** Ofertas activas de un docente que aun no terminan: las unicas que lo "ocupan". */
    List<T> findByDocenteIdAndActivoTrueAndFinAfter(Long docenteId, LocalDateTime ahora);

    long countByDocenteIdAndActivoTrueAndFinAfter(Long docenteId, LocalDateTime ahora);

    long countByModalidadIdAndActivoTrueAndFinAfter(Long modalidadId, LocalDateTime ahora);

    List<T> findByEspacioIdAndActivoTrueAndFinAfter(Long espacioId, LocalDateTime ahora);

    boolean existsByNombreIgnoreCaseAndCategoriaIdAndInicio(String nombre, Long categoriaId, LocalDateTime inicio);

    boolean existsByNombreIgnoreCaseAndCategoriaIdAndInicioAndIdNot(String nombre, Long categoriaId,
                                                                    LocalDateTime inicio, Long id);

    long countByActivoTrue();
}
