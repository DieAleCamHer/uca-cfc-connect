package edu.udb.ucacfc.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDuiNit(String duiNit);

    boolean existsByDuiNit(String duiNit);

    // Util para buscadores en el frontend (autocompletar por nombre)
    java.util.List<Cliente> findByNombreContainingIgnoreCase(String nombre);
}
