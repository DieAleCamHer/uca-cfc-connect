package edu.udb.ucacfc.seguridad;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UsuarioService {
    Usuario registrar(Usuario usuario);
    Usuario buscarPorId(Long id);
    Usuario buscarPorEmail(String email);
    List<Usuario> listar();
    Page<Usuario> listarPaginado(Pageable pageable);
    Usuario actualizar(Long id, Usuario datos);
    void desactivar(Long id);
}
