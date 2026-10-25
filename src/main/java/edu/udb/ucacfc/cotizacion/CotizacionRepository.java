package edu.udb.ucacfc.cotizacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long>, JpaSpecificationExecutor<Cotizacion> {

    List<Cotizacion> findByEstadoInAndFechaVencimientoBefore(Collection<EstadoCotizacion> estados, LocalDate hoy);

    long countByClienteIdAndEstadoIn(Long clienteId, Collection<EstadoCotizacion> estados);

    long countByEstado(EstadoCotizacion estado);

    long countByClienteIdAndEstado(Long clienteId, EstadoCotizacion estado);

    List<Cotizacion> findTop10ByClienteIdOrderByIdDesc(Long clienteId);
}
