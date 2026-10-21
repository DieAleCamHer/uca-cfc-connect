package edu.udb.ucacfc.agenda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActividadAgendaRepository extends JpaRepository<ActividadAgenda, Long>,
        JpaSpecificationExecutor<ActividadAgenda> {

    /** Actividades que BLOQUEAN el espacio y se cruzan con [inicio, fin): a.inicio < fin AND a.fin > inicio. */
    @Query("""
            select a from ActividadAgenda a
            where a.espacio.id = :espacioId and a.bloqueaEspacio = true
              and a.inicio < :fin and a.fin > :inicio
            order by a.inicio""")
    List<ActividadAgenda> cruces(@Param("espacioId") Long espacioId, @Param("inicio") LocalDateTime inicio,
                                 @Param("fin") LocalDateTime fin);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("delete from ActividadAgenda a where a.tipo = :tipo and a.referenciaId = :ref")
    int eliminar(@Param("tipo") TipoActividad tipo, @Param("ref") Long referenciaId);

    List<ActividadAgenda> findByTipoAndReferenciaIdOrderByInicio(TipoActividad tipo, Long referenciaId);

    /** Actividades futuras que usan un espacio (para impedir desactivarlo). */
    long countByEspacioIdAndBloqueaEspacioTrueAndFinAfter(Long espacioId, LocalDateTime ahora);
}
