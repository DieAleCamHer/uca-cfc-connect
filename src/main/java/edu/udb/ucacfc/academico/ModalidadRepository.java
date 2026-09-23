package edu.udb.ucacfc.academico;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModalidadRepository extends JpaRepository<Modalidad, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}
