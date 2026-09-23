package edu.udb.ucacfc.cliente;

import java.util.List;

public interface ClienteService {

    Cliente crear(Cliente cliente);

    Cliente buscarPorId(Long id);

    List<Cliente> listar();

    List<Cliente> buscarPorNombre(String nombre);

    Cliente actualizar(Long id, Cliente datos);

    void eliminar(Long id);
}
