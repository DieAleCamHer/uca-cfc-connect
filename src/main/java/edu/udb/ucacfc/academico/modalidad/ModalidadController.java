package edu.udb.ucacfc.academico.modalidad;

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
@RequestMapping("/api/modalidades")
@RequiredArgsConstructor
@Tag(name = "04. Academico - Modalidades", description = "Lectura: cualquier usuario autenticado. Escritura: ADMIN")
public class ModalidadController {

    private final ModalidadService service;

    @GetMapping
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Listar modalidads", description = "Filtros: q, activo. sort: id, nombre, requiereEspacioFisico, activo.")
    public PageResponse<ModalidadResponse> listar(@ParameterObject CatalogoFiltro filtro, @ParameterObject PaginaRequest pagina) {
        return service.listar(filtro, pagina);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Ver modalidad")
    public ModalidadResponse obtener(@PathVariable @Positive Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Crear modalidad", description = "Nombre unico (409). Se guarda en mayusculas.")
    public ModalidadResponse crear(@Valid @RequestBody ModalidadRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Editar modalidad", description = "409 si se intenta cambiar requiereEspacioFisico teniendo ofertas vigentes.")
    public ModalidadResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody ModalidadRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Desactivar modalidad", description = "Las ofertas existentes la conservan; no se podra usar en ofertas nuevas.")
    public void desactivar(@PathVariable @Positive Long id) {
        service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Reactivar modalidad")
    public void activar(@PathVariable @Positive Long id) {
        service.activar(id);
    }
}
