package edu.udb.ucacfc.academico;

import java.util.List;

public interface DiplomadoService {
    Diplomado crear(Diplomado diplomado);
    Diplomado buscarPorId(Long id);
    List<Diplomado> listar();
    List<Diplomado> listarActivos();
    Diplomado actualizar(Long id, Diplomado datos);
    void desactivar(Long id);
}
