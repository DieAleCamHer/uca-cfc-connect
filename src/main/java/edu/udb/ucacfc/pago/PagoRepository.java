package edu.udb.ucacfc.pago;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {

    Optional<Pago> findFirstByTipoReferenciaAndReferenciaIdAndEstadoNot(TipoReferenciaPago tipo, Long referenciaId,
                                                                       EstadoPago excluido);

    List<Pago> findByTipoReferenciaAndReferenciaIdOrderByIdDesc(TipoReferenciaPago tipo, Long referenciaId);

    /** Serializa abonos simultaneos a la misma cuenta (nunca se cobra de mas). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.id = :id")
    Optional<Pago> bloquear(@Param("id") Long id);

    List<Pago> findByClienteIdAndEstadoNot(Long clienteId, EstadoPago excluido);

    List<Pago> findByEstadoNot(EstadoPago excluido);
}
