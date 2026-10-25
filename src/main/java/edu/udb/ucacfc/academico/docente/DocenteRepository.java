package edu.udb.ucacfc.academico.docente;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocenteRepository extends JpaRepository<Docente, Long>, JpaSpecificationExecutor<Docente> {
    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    boolean existsByTelefono(String telefono);

    boolean existsByTelefonoAndIdNot(String telefono, Long id);

    /** Bloquea al docente mientras se valida su disponibilidad: dos ofertas simultaneas no pueden tomarlo a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Docente d where d.id = :id")
    Optional<Docente> bloquear(@Param("id") Long id);
}
