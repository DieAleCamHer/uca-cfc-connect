package edu.udb.ucacfc.seguridad.usuario;

import edu.udb.ucacfc.seguridad.usuario.dto.CambiarRolRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.PasswordAdminRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.RolResponse;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioActualizarRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioCrearRequest;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioFiltro;
import edu.udb.ucacfc.seguridad.usuario.dto.UsuarioResponse;
import edu.udb.ucacfc.shared.security.Permisos;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize(Permisos.ADMIN)
@Tag(name = "02. Usuarios y roles", description = "Solo ADMIN. Cuentas del personal, roles y permisos")
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping("/usuarios")
    @Operation(summary = "Listar cuentas (paginado y filtros)",
            description = "Filtros: q (nombre o correo), rol, activo. sort: id, nombre, email, rol, activo, ultimoAcceso, fechaCreacion.")
    public PageResponse<UsuarioResponse> listar(@ParameterObject UsuarioFiltro filtro,
                                                @ParameterObject PaginaRequest pagina) {
        return service.listar(filtro, pagina);
    }

    @GetMapping("/usuarios/{id}")
    @Operation(summary = "Ver una cuenta")
    public UsuarioResponse obtener(@PathVariable @Positive Long id) {
        return service.obtener(id);
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear cuenta de personal",
            description = "Rol ADMIN, RECEPCIONISTA o CONTABILIDAD. Las cuentas CLIENTE se crean con el registro publico "
                    + "o con POST /api/clientes/{id}/cuenta.")
    public UsuarioResponse crear(@Valid @RequestBody UsuarioCrearRequest request) {
        return service.crear(request);
    }

    @PutMapping("/usuarios/{id}")
    @Operation(summary = "Editar nombre y correo", description = "Si cambia el correo, se cierran sus sesiones.")
    public UsuarioResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody UsuarioActualizarRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/usuarios/{id}/rol")
    @Operation(summary = "Cambiar rol",
            description = "Solo entre roles de personal. No sobre ti mismo ni dejando al sistema sin ADMIN activo. Revoca sus tokens.")
    public UsuarioResponse cambiarRol(@PathVariable @Positive Long id, @Valid @RequestBody CambiarRolRequest request,
                                      @Parameter(hidden = true) @AuthenticationPrincipal Usuario actual) {
        return service.cambiarRol(id, request, actual);
    }

    @DeleteMapping("/usuarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar cuenta (borrado logico)",
            description = "No sobre ti mismo ni el ultimo ADMIN activo. La cuenta deja de poder iniciar sesion al instante.")
    public void desactivar(@PathVariable @Positive Long id,
                           @Parameter(hidden = true) @AuthenticationPrincipal Usuario actual) {
        service.desactivar(id, actual);
    }

    @PatchMapping("/usuarios/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reactivar cuenta")
    public void activar(@PathVariable @Positive Long id) {
        service.activar(id);
    }

    @PatchMapping("/usuarios/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Asignar contrasena temporal", description = "Para personal que olvido su contrasena. Revoca sus sesiones.")
    public void restablecerPassword(@PathVariable @Positive Long id, @Valid @RequestBody PasswordAdminRequest request) {
        service.restablecerPassword(id, request);
    }

    @GetMapping("/roles")
    @Operation(summary = "Roles y permisos del sistema", description = "Catalogo de roles con sus permisos y cuantas cuentas activas tiene cada uno.")
    public List<RolResponse> roles() {
        return service.roles();
    }
}
