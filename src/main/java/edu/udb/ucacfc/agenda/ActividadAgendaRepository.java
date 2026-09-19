package edu.udb.ucacfc.agenda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActividadAgendaRepository extends JpaRepository<ActividadAgenda, Long> {

    List<ActividadAgenda> findByTipo(TipoActividad tipo);

    List<ActividadAgenda> findByTipoAndReferenciaId(TipoActividad tipo, Long referenciaId);

    /**
     * Vista de agenda para un rango de fechas (ej: "que hay programado esta semana").
     * Recibe LocalDateTime (no LocalDate) porque fechaHoraInicio es LocalDateTime:
     * Hibernate no puede comparar un LocalDate contra una columna LocalDateTime
     * directamente. La conversion LocalDate -> LocalDateTime (inicio/fin del dia)
     * se hace en AgendaServiceImpl, no aqui.
     */
    @Query("""
            SELECT a FROM ActividadAgenda a
            WHERE a.fechaHoraInicio >= :desde AND a.fechaHoraInicio < :hasta
            ORDER BY a.fechaHoraInicio
            """)
    List<ActividadAgenda> findEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    /**
     * Igual que ReservaEspacioRepository.findSolapadas, pero a nivel de TODA
     * la agenda institucional: detecta cruces sin importar si la actividad
     * que ocupa el espacio es un Curso, Diplomado, Evento, Alquiler o Catering.
     */
    @Query("""
            SELECT a FROM ActividadAgenda a
            WHERE a.espacio.id = :espacioId
              AND a.fechaHoraInicio < :fin
              AND a.fechaHoraFin > :inicio
            """)
    List<ActividadAgenda> findSolapadasPorEspacio(
            @Param("espacioId") Long espacioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );
}
