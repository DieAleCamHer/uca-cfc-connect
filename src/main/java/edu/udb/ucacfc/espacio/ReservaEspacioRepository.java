package edu.udb.ucacfc.espacio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaEspacioRepository extends JpaRepository<ReservaEspacio, Long> {

    List<ReservaEspacio> findByClienteId(Long clienteId);

    List<ReservaEspacio> findByEspacioId(Long espacioId);

    /**
     * Devuelve las reservas del mismo espacio que se SOLAPAN con el rango
     * [inicio, fin) dado, ignorando las que ya estan CANCELADO.
     * Dos rangos se solapan si: inicioExistente < finNuevo Y finExistente > inicioNuevo.
     *
     * El Service la usa asi: si esta lista NO viene vacia, se lanza
     * EspacioOcupadoException y no se permite crear/confirmar la reserva.
     */
    @Query("""
            SELECT r FROM ReservaEspacio r
            WHERE r.espacio.id = :espacioId
              AND r.estado <> edu.udb.ucacfc.espacio.EstadoReserva.CANCELADO
              AND r.fechaHoraInicio < :fin
              AND r.fechaHoraFin > :inicio
            """)
    List<ReservaEspacio> findSolapadas(
            @Param("espacioId") Long espacioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    /**
     * Misma logica, pero excluyendo una reserva puntual (id). Se usa cuando
     * se esta EDITANDO una reserva existente: no debe chocar consigo misma.
     */
    @Query("""
            SELECT r FROM ReservaEspacio r
            WHERE r.espacio.id = :espacioId
              AND r.id <> :idExcluir
              AND r.estado <> edu.udb.ucacfc.espacio.EstadoReserva.CANCELADO
              AND r.fechaHoraInicio < :fin
              AND r.fechaHoraFin > :inicio
            """)
    List<ReservaEspacio> findSolapadasExcluyendo(
            @Param("espacioId") Long espacioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("idExcluir") Long idExcluir
    );
}
