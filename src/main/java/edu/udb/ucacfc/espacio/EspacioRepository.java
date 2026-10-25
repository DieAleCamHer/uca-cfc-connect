package edu.udb.ucacfc.espacio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EspacioRepository extends JpaRepository<Espacio, Long>, JpaSpecificationExecutor<Espacio> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /**
     * SELECT ... FOR UPDATE: mientras dure la transaccion nadie mas puede registrar actividades en este espacio.
     * Evita que dos reservas simultaneas del mismo horario pasen ambas la validacion de cruce.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Espacio e where e.id = :id")
    Optional<Espacio> bloquear(@Param("id") Long id);
}
