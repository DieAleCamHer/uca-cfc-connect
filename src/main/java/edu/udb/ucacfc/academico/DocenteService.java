package edu.udb.ucacfc.academico;

import java.util.List;

public interface DocenteService {
    Docente crear(Docente docente);
    Docente buscarPorId(Long id);
    List<Docente> listar();
    Docente actualizar(Long id, Docente datos);
    void eliminar(Long id);
}
