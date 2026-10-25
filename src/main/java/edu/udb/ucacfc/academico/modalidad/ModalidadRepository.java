package edu.udb.ucacfc.academico.modalidad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ModalidadRepository extends JpaRepository<Modalidad, Long>, JpaSpecificationExecutor<Modalidad> {
    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
