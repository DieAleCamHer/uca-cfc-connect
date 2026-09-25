package edu.udb.ucacfc.catering;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SolicitudCateringRepository extends JpaRepository<SolicitudCatering, Long> {

    List<SolicitudCatering> findByClienteId(Long clienteId);

    List<SolicitudCatering> findByFecha(LocalDate fecha);

    List<SolicitudCatering> findByEstado(EstadoSolicitudCatering estado);
}
