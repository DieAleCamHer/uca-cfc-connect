package edu.udb.ucacfc.agenda;

import edu.udb.ucacfc.agenda.dto.ActividadResponse;
import edu.udb.ucacfc.agenda.dto.AgendaFiltro;
import edu.udb.ucacfc.agenda.dto.EventoFiltro;
import edu.udb.ucacfc.agenda.dto.EventoRequest;
import edu.udb.ucacfc.agenda.dto.EventoResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agenda")
@RequiredArgsConstructor
@Tag(name = "15. Agenda institucional", description = "Calendario unico de cursos, diplomados, eventos, alquileres y catering "
        + "(RF13). Las actividades las escriben los modulos; aqui solo se programan eventos propios del CFC.")
public class AgendaController {

    private final AgendaService agendaService;
    private final EventoService eventoService;

    @GetMapping
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Consultar calendario",
            description = "Rango desde/hasta obligatorio (maximo 93 dias). Filtros: tipo, espacioId. Un CLIENTE ve cursos, "
                    + "diplomados y eventos, y solo SUS alquileres y catering.")
    public List<ActividadResponse> consultar(@ParameterObject AgendaFiltro filtro,
                                             @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return agendaService.consultar(filtro, usuario);
    }

    @GetMapping("/actividades/{id}")
    @PreAuthorize(Permisos.STAFF_Y_CLIENTE)
    @Operation(summary = "Ver una actividad del calendario")
    public ActividadResponse actividad(@PathVariable @Positive Long id,
                                       @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuario) {
        return agendaService.obtener(id, usuario);
    }

    @GetMapping("/eventos")
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Listar eventos institucionales", description = "Filtros: q, estado, espacioId, desde/hasta. sort: id, inicio, titulo, estado.")
    public PageResponse<EventoResponse> eventos(@ParameterObject EventoFiltro filtro, @ParameterObject PaginaRequest pagina) {
        return eventoService.listar(filtro, pagina);
    }

    @GetMapping("/eventos/{id}")
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Ver evento")
    public EventoResponse evento(@PathVariable @Positive Long id) {
        return eventoService.obtener(id);
    }

    @PostMapping("/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Programar evento (ADMIN, RECEPCIONISTA)",
            description = "Futuro, 30 min a 12 h. Con espacio: lo bloquea (409 SPACE_OCCUPIED si choca); sin espacio: lugarExterno obligatorio.")
    public EventoResponse crearEvento(@Valid @RequestBody EventoRequest request) {
        return eventoService.crear(request);
    }

    @PutMapping("/eventos/{id}")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Reprogramar evento (ADMIN, RECEPCIONISTA)", description = "Solo antes de que inicie.")
    public EventoResponse actualizarEvento(@PathVariable @Positive Long id, @Valid @RequestBody EventoRequest request) {
        return eventoService.actualizar(id, request);
    }

    @DeleteMapping("/eventos/{id}")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Cancelar evento (ADMIN, RECEPCIONISTA)", description = "Libera el espacio. Solo antes de que inicie.")
    public EventoResponse cancelarEvento(@PathVariable @Positive Long id,
                                         @RequestParam(required = false) @Size(max = 300) String motivo) {
        return eventoService.cancelar(id, motivo);
    }
}
