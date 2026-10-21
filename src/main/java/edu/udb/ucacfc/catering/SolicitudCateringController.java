package edu.udb.ucacfc.catering;

import edu.udb.ucacfc.catering.dto.SolicitudCateringFiltro;
import edu.udb.ucacfc.catering.dto.SolicitudCateringRequest;
import edu.udb.ucacfc.catering.dto.SolicitudCateringResponse;
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
@RequestMapping("/api/solicitudes-catering")
@RequiredArgsConstructor
@Tag(name = "13. Catering - Solicitudes",
        description = "PENDIENTE -> CONFIRMADA -> ATENDIDA; PENDIENTE -> RECHAZADA; PENDIENTE/CONFIRMADA -> CANCELADA. "
                + "El costo (precio por persona x asistentes) lo calcula el sistema.")
public class SolicitudCateringController {

    private final SolicitudCateringService service;

    @GetMapping
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Listar solicitudes de catering", description = "Filtros: clienteId (ignorado para CLIENTE), servicioId, "
            + "estado, desde/hasta. sort: id, inicio, estado, total, numeroAsistentes, cliente, servicio.")
    public PageResponse<SolicitudCateringResponse> listar(@ParameterObject SolicitudCateringFiltro filtro, @ParameterObject PaginaRequest pagina,
                                                @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.listar(filtro, pagina, usuario);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Ver solicitud de catering")
    public SolicitudCateringResponse obtener(@PathVariable @Positive Long id,
                                   @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.obtener(id, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Solicitar / registrar catering (RF08)",
            description = "Con 24 h de anticipacion, mismo dia, maximo 8 h. Lugar: reservaId (alquiler del mismo cliente que cubra "
                    + "el horario), espacioId (espacio libre: 409 SPACE_OCCUPIED si choca) o lugar externo. "
                    + "Asistentes <= capacidad. Total = precio por persona x asistentes.")
    public SolicitudCateringResponse crear(@Valid @RequestBody SolicitudCateringRequest request,
                                 @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.crear(request, usuario);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Editar solicitud PENDIENTE", description = "Revalida todo y recalcula el costo.")
    public SolicitudCateringResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody SolicitudCateringRequest request,
                                      @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.actualizar(id, request, usuario);
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Confirmar (ADMIN, RECEPCIONISTA)", description = "PENDIENTE -> CONFIRMADA; genera la cuenta por cobrar.")
    public SolicitudCateringResponse confirmar(@PathVariable @Positive Long id,
                                     @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.confirmar(id, usuario);
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Rechazar (ADMIN, RECEPCIONISTA)", description = "PENDIENTE -> RECHAZADA con motivo; libera el espacio.")
    public SolicitudCateringResponse rechazar(@PathVariable @Positive Long id, @Valid @RequestBody MotivoObligatorioRequest body,
                                    @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.rechazar(id, body.motivo(), usuario);
    }

    @PatchMapping("/{id}/atender")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Marcar ATENDIDA (ADMIN, RECEPCIONISTA)",
            description = "Respaldo del cierre automatico: el evento debe haber iniciado y la cuenta estar PAGADA.")
    public SolicitudCateringResponse atender(@PathVariable @Positive Long id,
                                             @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.atender(id, usuario);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Cancelar solicitud",
            description = "Libera el espacio y anula la cuenta. CLIENTE: solo la suya y hasta 24 h antes. 409 si ya inicio o "
                    + "si hay dinero cobrado.")
    public SolicitudCateringResponse cancelar(@PathVariable @Positive Long id,
                                    @RequestParam(required = false) @Size(max = 300) String motivo,
                                    @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.cancelar(id, motivo, usuario);
    }
}
