package edu.udb.ucacfc.academico;

import java.util.List;

public interface CategoriaService {
    Categoria crear(Categoria categoria);
    Categoria buscarPorId(Long id);
    List<Categoria> listar();
    Categoria actualizar(Long id, Categoria datos);
    void eliminar(Long id);
}
