package edu.udb.ucacfc.cotizacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    List<Cotizacion> findByClienteId(Long clienteId);

    List<Cotizacion> findByEstado(EstadoCotizacion estado);

    List<Cotizacion> findByTipo(TipoCotizacion tipo);
}
