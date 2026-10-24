package edu.udb.ucacfc.cliente;

import edu.udb.ucacfc.cliente.dto.ClienteFiltro;
import edu.udb.ucacfc.cliente.dto.ClientePerfilRequest;
import edu.udb.ucacfc.cliente.dto.ClienteRequest;
import edu.udb.ucacfc.cliente.dto.ClienteResponse;
import edu.udb.ucacfc.cliente.dto.CrearCuentaRequest;
import edu.udb.ucacfc.cliente.dto.HistorialClienteResponse;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
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

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "08. Clientes", description = "Registro, actualizacion, busqueda e historial de clientes (personas y empresas)")
public class ClienteController {

    private final ClienteService service;

    @GetMapping
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Buscar clientes (ADMIN, RECEPCIONISTA)",
            description = "Filtros: q (nombre, empresa, DUI/NIT, correo), tipo, activo, conCuenta. sort: id, nombre, "
                    + "empresa, duiNit, correo, tipo, activo, fechaCreacion.")
    public PageResponse<ClienteResponse> listar(@ParameterObject ClienteFiltro filtro, @ParameterObject PaginaRequest pagina) {
        return service.listar(filtro, pagina);
    }

    @GetMapping("/me")
    @PreAuthorize(Permisos.CLIENTE)
    @Operation(summary = "Mi ficha de cliente (CLIENTE)")
    public ClienteResponse miFicha(@Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.miFicha(usuario);
    }

    @PutMapping("/me")
    @PreAuthorize(Permisos.CLIENTE)
    @Operation(summary = "Actualizar mi ficha (CLIENTE)", description = "Nombre, empresa, correo, telefono y direccion. "
            + "El tipo y el DUI/NIT solo los corrige el personal.")
    public ClienteResponse actualizarMiFicha(@Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario,
                                             @Valid @RequestBody ClientePerfilRequest request) {
        return service.actualizarMiFicha(usuario, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Ver cliente", description = "Un CLIENTE solo puede ver su propia ficha (403 si es otra).")
    public ClienteResponse obtener(@PathVariable @Positive Long id,
                                   @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.obtener(id, usuario);
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Historial del cliente (RF14)",
            description = "Conteo por estado de inscripciones, reservas, catering y cotizaciones, resumen financiero y "
                    + "ultimos movimientos. Un CLIENTE solo el suyo.")
    public HistorialClienteResponse historial(@PathVariable @Positive Long id,
                                              @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.historial(id, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Registrar cliente (ADMIN, RECEPCIONISTA)",
            description = "Cliente de mostrador, sin cuenta de acceso. EMPRESA exige razon social y NIT. DUI/NIT, correo y "
                    + "telefono unicos (409). Los clientes que se registran solos usan POST /api/auth/registro.")
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Actualizar cliente (ADMIN, RECEPCIONISTA)", description = "El tipo de cliente no se puede cambiar (409).")
    public ClienteResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody ClienteRequest request) {
        return service.actualizar(id, request);
    }

    @PostMapping("/{id}/cuenta")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Crear acceso web para un cliente de mostrador (ADMIN, RECEPCIONISTA)",
            description = "Crea una cuenta con rol CLIENTE unida a esta ficha. 409 si ya tiene cuenta o el correo existe.")
    public ClienteResponse crearCuenta(@PathVariable @Positive Long id, @Valid @RequestBody CrearCuentaRequest request) {
        return service.crearCuenta(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Desactivar cliente (ADMIN)",
            description = "409 si tiene procesos vigentes o saldo pendiente. Tambien desactiva su cuenta de acceso.")
    public void desactivar(@PathVariable @Positive Long id) {
        service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Reactivar cliente (ADMIN)", description = "Reactiva tambien su cuenta de acceso si la tiene.")
    public void activar(@PathVariable @Positive Long id) {
        service.activar(id);
    }
}
