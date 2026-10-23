package edu.udb.ucacfc.inscripcion;

import edu.udb.ucacfc.inscripcion.dto.InscripcionFiltro;
import edu.udb.ucacfc.inscripcion.dto.InscripcionRequest;
import edu.udb.ucacfc.inscripcion.dto.InscripcionResponse;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.shared.security.Permisos;
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
@RequestMapping("/api/inscripciones")
@RequiredArgsConstructor
@Tag(name = "09. Inscripciones", description = "PENDIENTE -> CONFIRMADA -> FINALIZADA; PENDIENTE/CONFIRMADA -> CANCELADA. "
        + "Un CLIENTE ve, crea, edita y cancela solo lo suyo; confirmar y finalizar es del personal.")
public class InscripcionController {

    private final InscripcionService service;

    @GetMapping
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Listar inscripciones", description = "Filtros: clienteId (ignorado para CLIENTE), cursoId, "
            + "diplomadoId, estado, q (participante o cliente), desde/hasta. sort: id, estado, fechaInscripcion, monto, cliente.")
    public PageResponse<InscripcionResponse> listar(@ParameterObject InscripcionFiltro filtro,
                                                    @ParameterObject PaginaRequest pagina,
                                                    @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.listar(filtro, pagina, usuario);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Ver inscripcion", description = "Incluye el estado de su cuenta y las acciones que tu rol puede hacer.")
    public InscripcionResponse obtener(@PathVariable @Positive Long id,
                                       @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.obtener(id, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Inscribir participante",
            description = "Exactamente uno: cursoId o diplomadoId. Cliente EMPRESA: participanteNombre y participanteDui "
                    + "obligatorios. 409 CAPACITY_EXCEEDED sin cupo, 409 DUPLICATE si ya tiene inscripcion vigente, "
                    + "400 si la oferta esta desactivada o finalizada. Nace PENDIENTE con el costo vigente congelado.")
    public InscripcionResponse crear(@Valid @RequestBody InscripcionRequest request,
                                     @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.crear(request, usuario);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Editar inscripcion PENDIENTE", description = "Cambiar de oferta o corregir el participante. "
            + "El cliente no cambia. Revalida cupo y duplicados.")
    public InscripcionResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody InscripcionRequest request,
                                          @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.actualizar(id, request, usuario);
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Confirmar (ADMIN, RECEPCIONISTA)", description = "PENDIENTE -> CONFIRMADA y genera la cuenta por cobrar.")
    public InscripcionResponse confirmar(@PathVariable @Positive Long id,
                                         @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.confirmar(id, usuario);
    }

    @PatchMapping("/{id}/finalizar")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Finalizar manualmente (ADMIN, RECEPCIONISTA)",
            description = "Respaldo del cierre automatico: exige que la oferta haya terminado y la cuenta este PAGADA.")
    public InscripcionResponse finalizar(@PathVariable @Positive Long id,
                                         @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.finalizar(id, usuario);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Cancelar inscripcion",
            description = "Libera el cupo y anula la cuenta. 409 si hay dinero cobrado o en validacion (no hay reembolsos). "
                    + "Un CLIENTE solo la suya y solo antes de que inicie la oferta.")
    public InscripcionResponse cancelar(@PathVariable @Positive Long id,
                                        @RequestParam(required = false) @Size(max = 300) String motivo,
                                        @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return service.cancelar(id, motivo, usuario);
    }
}
