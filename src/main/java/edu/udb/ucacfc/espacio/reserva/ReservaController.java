package edu.udb.ucacfc.espacio.reserva;

import edu.udb.ucacfc.espacio.reserva.dto.ReservaFiltro;
import edu.udb.ucacfc.espacio.reserva.dto.ReservaRequest;
import edu.udb.ucacfc.espacio.reserva.dto.ReservaResponse;
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
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "11. Alquiler de espacios",
        description = "SOLICITADA -> CONFIRMADA -> COMPLETADA; SOLICITADA -> RECHAZADA; SOLICITADA/CONFIRMADA -> CANCELADA. "
                + "Una reserva SOLICITADA ya bloquea el espacio en la agenda.")
public class ReservaController {

    private final ReservaService service;

    @GetMapping
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Listar reservas", description = "Filtros: clienteId (ignorado para CLIENTE), espacioId, estado, "
            + "desde/hasta. sort: id, inicio, fin, estado, total, espacio, cliente.")
    public PageResponse<ReservaResponse> listar(@ParameterObject ReservaFiltro filtro, @ParameterObject PaginaRequest pagina,
                                                @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.listar(filtro, pagina, usuario);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Ver reserva")
    public ReservaResponse obtener(@PathVariable @Positive Long id,
                                   @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.obtener(id, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Solicitar / registrar alquiler (RF07)",
            description = "Futura, de 30 min a 12 h, asistentes <= capacidad. 409 SPACE_OCCUPIED si choca con cualquier "
                    + "actividad de la agenda. Costo = horas x precio por hora.")
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest request,
                                 @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.crear(request, usuario);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Editar reserva SOLICITADA", description = "Revalida horario, capacidad y recalcula el costo.")
    public ReservaResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody ReservaRequest request,
                                      @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.actualizar(id, request, usuario);
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Confirmar (ADMIN, RECEPCIONISTA)", description = "SOLICITADA -> CONFIRMADA; genera la cuenta por cobrar.")
    public ReservaResponse confirmar(@PathVariable @Positive Long id,
                                     @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.confirmar(id, usuario);
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Rechazar (ADMIN, RECEPCIONISTA)", description = "SOLICITADA -> RECHAZADA con motivo; libera el espacio.")
    public ReservaResponse rechazar(@PathVariable @Positive Long id, @Valid @RequestBody MotivoObligatorioRequest body,
                                    @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.rechazar(id, body.motivo(), usuario);
    }

    @PatchMapping("/{id}/completar")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Completar manualmente (ADMIN, RECEPCIONISTA)",
            description = "Respaldo del cierre automatico: exige horario terminado y cuenta PAGADA.")
    public ReservaResponse completar(@PathVariable @Positive Long id,
                                     @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.completar(id, usuario);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Cancelar reserva",
            description = "Libera el espacio y anula la cuenta. CLIENTE: solo la suya y hasta 24 h antes. 409 si ya inicio, "
                    + "si hay dinero cobrado o si tiene catering vigente asociado.")
    public ReservaResponse cancelar(@PathVariable @Positive Long id,
                                    @RequestParam(required = false) @Size(max = 300) String motivo,
                                    @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.cancelar(id, motivo, usuario);
    }
}
