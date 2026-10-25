package edu.udb.ucacfc.cotizacion;

import edu.udb.ucacfc.cotizacion.dto.CotizacionFiltro;
import edu.udb.ucacfc.cotizacion.dto.CotizacionRequest;
import edu.udb.ucacfc.cotizacion.dto.CotizacionResponse;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.shared.security.Permisos;
import edu.udb.ucacfc.shared.web.MotivoObligatorioRequest;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cotizaciones")
@RequiredArgsConstructor
@Tag(name = "14. Cotizaciones", description = "PENDIENTE -> EN_PROCESO -> APROBADA (solo ADMIN) | RECHAZADA. "
        + "Lineas con referencias al catalogo; el precio sugerido sale del catalogo y solo el personal lo negocia.")
public class CotizacionController {

    private final CotizacionService service;

    @GetMapping
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Listar cotizaciones", description = "Filtros: clienteId (ignorado para CLIENTE), tipo, estado, q, "
            + "desde/hasta. sort: id, estado, tipo, total, fechaSolicitud, fechaVencimiento, cliente.")
    public PageResponse<CotizacionResponse> listar(@ParameterObject CotizacionFiltro filtro, @ParameterObject PaginaRequest pagina,
                                                   @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.listar(filtro, pagina, usuario);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Ver cotizacion con sus lineas")
    public CotizacionResponse obtener(@PathVariable @Positive Long id,
                                      @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.obtener(id, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Solicitar (CLIENTE) o registrar (personal) una cotizacion",
            description = "Tipos y lineas admitidas: CURSO_EMPRESARIAL->CURSO, DIPLOMADO->DIPLOMADO, ALQUILER_ESPACIO->ESPACIO, "
                    + "CATERING->CATERING, SERVICIO_COMBINADO->al menos dos distintos; OTRO siempre. Lo referenciado debe "
                    + "estar activo. precioUnitario, descuento y fechaVencimiento solo los fija el personal.")
    public CotizacionResponse crear(@Valid @RequestBody CotizacionRequest request,
                                    @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.crear(request, usuario);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Editar cotizacion", description = "CLIENTE: la suya y solo PENDIENTE. Personal: PENDIENTE o EN_PROCESO "
            + "(aqui ajusta precios, descuento y vigencia).")
    public CotizacionResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody CotizacionRequest request,
                                         @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.actualizar(id, request, usuario);
    }

    @PatchMapping("/{id}/en-proceso")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Tomar para elaborar (ADMIN, RECEPCIONISTA)", description = "PENDIENTE -> EN_PROCESO.")
    public CotizacionResponse enProceso(@PathVariable @Positive Long id,
                                        @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.enProceso(id, usuario);
    }

    @PatchMapping("/{id}/aprobar")
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Aprobar (solo ADMIN)", description = "Exige total > 0, vigencia no vencida, lineas OTRO con precio y "
            + "catalogo activo. Genera la cuenta por cobrar por el total.")
    public CotizacionResponse aprobar(@PathVariable @Positive Long id,
                                      @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.aprobar(id, usuario);
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Rechazar (ADMIN, RECEPCIONISTA)", description = "PENDIENTE/EN_PROCESO -> RECHAZADA con motivo.")
    public CotizacionResponse rechazar(@PathVariable @Positive Long id, @Valid @RequestBody MotivoObligatorioRequest body,
                                       @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.rechazar(id, body.motivo(), usuario);
    }

    @PatchMapping("/{id}/anular-aprobacion")
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Anular una aprobacion (solo ADMIN)", description = "APROBADA -> RECHAZADA y anula la cuenta. "
            + "409 si ya hay dinero cobrado o en validacion.")
    public CotizacionResponse anularAprobacion(@PathVariable @Positive Long id, @Valid @RequestBody MotivoObligatorioRequest body,
                                               @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.anularAprobacion(id, body.motivo(), usuario);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Retirar solicitud", description = "PENDIENTE/EN_PROCESO -> RECHAZADA. Un CLIENTE solo la suya.")
    public CotizacionResponse retirar(@PathVariable @Positive Long id,
                                      @RequestParam(required = false) @Size(max = 300) String motivo,
                                      @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.retirar(id, motivo, usuario);
    }
}
