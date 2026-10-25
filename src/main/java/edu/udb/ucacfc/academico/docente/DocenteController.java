package edu.udb.ucacfc.academico.docente;

import edu.udb.ucacfc.shared.security.Permisos;
import edu.udb.ucacfc.shared.web.CatalogoFiltro;
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
@RequestMapping("/api/docentes")
@RequiredArgsConstructor
@Tag(name = "05. Academico - Docentes", description = "Lectura: cualquier usuario autenticado. Escritura: ADMIN")
public class DocenteController {

    private final DocenteService service;

    @GetMapping
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Listar docentes", description = "Filtros: q, activo. q busca en nombre, especialidad y correo. sort: id, nombre, especialidad, correo, activo.")
    public PageResponse<DocenteResponse> listar(@ParameterObject CatalogoFiltro filtro, @ParameterObject PaginaRequest pagina) {
        return service.listar(filtro, pagina);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Ver docente")
    public DocenteResponse obtener(@PathVariable @Positive Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Crear docente", description = "Correo y telefono unicos (409).")
    public DocenteResponse crear(@Valid @RequestBody DocenteRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Editar docente")
    public DocenteResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody DocenteRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Desactivar docente", description = "409 si tiene ofertas vigentes asignadas (hay que reasignarlas primero).")
    public void desactivar(@PathVariable @Positive Long id) {
        service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Reactivar docente")
    public void activar(@PathVariable @Positive Long id) {
        service.activar(id);
    }
}
