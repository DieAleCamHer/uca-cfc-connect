package edu.udb.ucacfc.espacio.reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservaEspacioRepository extends JpaRepository<ReservaEspacio, Long>,
        JpaSpecificationExecutor<ReservaEspacio> {

    List<ReservaEspacio> findByEstadoAndFinBefore(EstadoReserva estado, LocalDateTime ahora);

    List<ReservaEspacio> findByEstadoAndInicioBefore(EstadoReserva estado, LocalDateTime ahora);

    long countByClienteIdAndEstadoIn(Long clienteId, Collection<EstadoReserva> estados);

    long countByEstado(EstadoReserva estado);

    boolean existsByEspacioIdAndEstadoInAndFinAfterAndNumeroAsistentesGreaterThan(Long espacioId,
            Collection<EstadoReserva> estados, LocalDateTime ahora, Integer asistentes);

    long countByClienteIdAndEstado(Long clienteId, EstadoReserva estado);

    List<ReservaEspacio> findTop10ByClienteIdOrderByIdDesc(Long clienteId);
}
