package edu.udb.ucacfc.academico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findByActivoTrue();

    List<Curso> findByCategoriaId(Long categoriaId);

    List<Curso> findByDocenteId(Long docenteId);
}
