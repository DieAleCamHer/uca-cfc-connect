package edu.udb.ucacfc.academico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiplomadoRepository extends JpaRepository<Diplomado, Long> {

    List<Diplomado> findByActivoTrue();

    List<Diplomado> findByCategoriaId(Long categoriaId);

    List<Diplomado> findByDocenteId(Long docenteId);
}
