package edu.udb.ucacfc.catering;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SolicitudCateringRepository extends JpaRepository<SolicitudCatering, Long>,
        JpaSpecificationExecutor<SolicitudCatering> {

    List<SolicitudCatering> findByEstadoAndFinBefore(EstadoSolicitudCatering estado, LocalDateTime ahora);

    List<SolicitudCatering> findByEstadoAndInicioBefore(EstadoSolicitudCatering estado, LocalDateTime ahora);

    List<SolicitudCatering> findByReservaIdAndEstadoIn(Long reservaId, Collection<EstadoSolicitudCatering> estados);

    long countByServicioIdAndEstadoInAndInicioAfter(Long servicioId, Collection<EstadoSolicitudCatering> estados,
                                                    LocalDateTime ahora);

    long countByClienteIdAndEstadoIn(Long clienteId, Collection<EstadoSolicitudCatering> estados);

    long countByEstado(EstadoSolicitudCatering estado);

    boolean existsByEspacioIdAndEstadoInAndFinAfterAndNumeroAsistentesGreaterThan(Long espacioId,
            Collection<EstadoSolicitudCatering> estados, LocalDateTime ahora, Integer asistentes);

    long countByClienteIdAndEstado(Long clienteId, EstadoSolicitudCatering estado);

    List<SolicitudCatering> findTop10ByClienteIdOrderByIdDesc(Long clienteId);
}
