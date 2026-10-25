package edu.udb.ucacfc.academico.categoria;

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
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
@Tag(name = "03. Academico - Categorias", description = "Lectura: cualquier usuario autenticado. Escritura: ADMIN")
public class CategoriaController {

    private final CategoriaService service;

    @GetMapping
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Listar categorias", description = "Filtros: q, activo. sort: id, nombre, activo.")
    public PageResponse<CategoriaResponse> listar(@ParameterObject CatalogoFiltro filtro, @ParameterObject PaginaRequest pagina) {
        return service.listar(filtro, pagina);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Ver categoria")
    public CategoriaResponse obtener(@PathVariable @Positive Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Crear categoria", description = "Nombre unico sin distinguir mayusculas (409).")
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Editar categoria")
    public CategoriaResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody CategoriaRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Desactivar categoria", description = "Las ofertas existentes la conservan; no se podra usar en ofertas nuevas.")
    public void desactivar(@PathVariable @Positive Long id) {
        service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Reactivar categoria")
    public void activar(@PathVariable @Positive Long id) {
        service.activar(id);
    }
}
