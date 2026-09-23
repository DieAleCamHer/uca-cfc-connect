package edu.udb.ucacfc.academico;

import java.util.List;

public interface ModalidadService {
    Modalidad crear(Modalidad modalidad);
    Modalidad buscarPorId(Long id);
    List<Modalidad> listar();
    Modalidad actualizar(Long id, Modalidad datos);
    void eliminar(Long id);
}
