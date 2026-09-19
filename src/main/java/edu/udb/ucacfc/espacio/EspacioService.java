package edu.udb.ucacfc.espacio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EspacioService {
    Espacio crear(Espacio espacio);
    Espacio buscarPorId(Long id);
    List<Espacio> listar();
    Page<Espacio> listarPaginado(Pageable pageable);
    List<Espacio> listarDisponibles();
    Espacio actualizar(Long id, Espacio datos);
    void eliminar(Long id);
}
