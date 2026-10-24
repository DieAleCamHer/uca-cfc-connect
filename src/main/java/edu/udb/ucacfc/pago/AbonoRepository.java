package edu.udb.ucacfc.pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AbonoRepository extends JpaRepository<Abono, Long>, JpaSpecificationExecutor<Abono> {

    List<Abono> findByPagoIdOrderByFechaRegistroAsc(Long pagoId);

    boolean existsByPagoIdAndEstado(Long pagoId, EstadoAbono estado);

    @Query("select coalesce(sum(a.monto), 0) from Abono a where a.pago.id = :pagoId and a.estado = :estado")
    BigDecimal sumarPorEstado(@Param("pagoId") Long pagoId, @Param("estado") EstadoAbono estado);

    boolean existsByMetodoAndReferenciaExternaIgnoreCaseAndEstadoIn(MetodoPago metodo, String referencia,
                                                                     java.util.Collection<EstadoAbono> estados);

    long countByEstado(EstadoAbono estado);
}
