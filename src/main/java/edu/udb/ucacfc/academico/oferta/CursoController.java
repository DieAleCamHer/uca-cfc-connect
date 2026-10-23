package edu.udb.ucacfc.academico.oferta;

import edu.udb.ucacfc.academico.oferta.dto.OfertaFiltro;
import edu.udb.ucacfc.academico.oferta.dto.OfertaRequest;
import edu.udb.ucacfc.academico.oferta.dto.OfertaResponse;
import edu.udb.ucacfc.shared.security.Permisos;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
@Tag(name = "06. Academico - Cursos",
        description = "Lectura: todos los roles. Crear/editar: ADMIN y RECEPCIONISTA (programan actividades). "
                + "Desactivar/reactivar: ADMIN")
public class CursoController {

    private final CursoService service;

    @GetMapping
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Listar cursos (paginado y filtros)",
            description = "Filtros: q, categoriaId, modalidadId, docenteId, espacioId, estado (PROGRAMADA, EN_CURSO, "
                    + "FINALIZADA, INACTIVA), desde/hasta, conCupo. sort: id, nombre, inicio, fin, costo, cupoMaximo, "
                    + "activo, categoria, docente, modalidad.")
    public PageResponse<OfertaResponse> listar(@ParameterObject OfertaFiltro filtro, @ParameterObject PaginaRequest pagina) {
        return service.listar(filtro, pagina);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Ver curso con sus sesiones, inscritos y cupo disponible")
    public OfertaResponse obtener(@PathVariable @Positive Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Crear curso",
            description = "Sesiones futuras de 30 min a 12 h sin cruzarse. Presencial: espacio obligatorio, activo y con "
                    + "capacidad >= cupo; virtual: sin espacio (400). Docente con cruce de horario -> 409 TEACHER_BUSY. "
                    + "Espacio ocupado en alguna sesion -> 409 SPACE_OCCUPIED (no se guarda nada). Nombre + categoria + "
                    + "fecha de inicio unicos.")
    public OfertaResponse crear(@Valid @RequestBody OfertaRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.STAFF)
    @Operation(summary = "Editar curso",
            description = "Antes de iniciar: se puede cambiar todo (se reprograma la agenda). Ya iniciado: solo nombre, "
                    + "descripcion, cupo y costo. Finalizado o desactivado: 409. El cupo no baja de los inscritos vigentes; "
                    + "el costo nuevo solo aplica a inscripciones futuras.")
    public OfertaResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody OfertaRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Desactivar curso (baja en cascada)",
            description = "Si aun no termina: cancela sus inscripciones vigentes, anula sus cuentas y libera la agenda. "
                    + "Todo o nada: si una inscripcion tiene dinero cobrado o en validacion -> 409 y no cambia nada. "
                    + "Si ya termino: solo se oculta.")
    public void desactivar(@PathVariable @Positive Long id) {
        service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Reactivar curso",
            description = "Revalida catalogos activos, disponibilidad del docente y del espacio (409 si alguien los tomo). "
                    + "Las inscripciones canceladas por la baja no se restauran.")
    public void activar(@PathVariable @Positive Long id) {
        service.activar(id);
    }
}
