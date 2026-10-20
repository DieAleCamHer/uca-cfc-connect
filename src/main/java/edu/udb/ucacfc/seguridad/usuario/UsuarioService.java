package edu.udb.ucacfc.seguridad.usuario;

import edu.udb.ucacfc.seguridad.usuario.dto.CambiarRolRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.PasswordAdminRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.RolResponse;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioActualizarRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioCrearRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioFiltro;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioResponse;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;

import java.util.List;

public interface UsuarioService {
    PageResponse<UsuarioResponse> listar(UsuarioFiltro filtro, PaginaRequest pagina);

    UsuarioResponse obtener(Long id);

    UsuarioResponse crear(UsuarioCrearRequest request);

    UsuarioResponse actualizar(Long id, UsuarioActualizarRequest request);

    UsuarioResponse cambiarRol(Long id, CambiarRolRequest request, Usuario actual);

    void desactivar(Long id, Usuario actual);

    void activar(Long id);

    void restablecerPassword(Long id, PasswordAdminRequest request);

    List<RolResponse> roles();
}
