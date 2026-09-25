package edu.udb.ucacfc.academico;

import java.util.List;

public interface CursoService {
    Curso crear(Curso curso);
    Curso buscarPorId(Long id);
    List<Curso> listar();
    List<Curso> listarActivos();
    Curso actualizar(Long id, Curso datos);
    void desactivar(Long id);
}
