package edu.udb.ucacfc.espacio;

import edu.udb.ucacfc.espacio.dto.DisponibilidadFiltro;
import edu.udb.ucacfc.espacio.dto.DisponibilidadResponse;
import edu.udb.ucacfc.espacio.dto.EspacioFiltro;
import edu.udb.ucacfc.espacio.dto.EspacioRequest;
import edu.udb.ucacfc.espacio.dto.EspacioResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
@RequiredArgsConstructor
@Tag(name = "10. Espacios", description = "Catalogo de espacios y consulta de disponibilidad. Escritura: ADMIN")
public class EspacioController {

    private final EspacioService service;

    @GetMapping
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Listar espacios", description = "Filtros: q, tipo, activo, capacidadMinima. sort: id, nombre, "
            + "tipo, capacidad, precioHora, activo.")
    public PageResponse<EspacioResponse> listar(@ParameterObject EspacioFiltro filtro, @ParameterObject PaginaRequest pagina) {
        return service.listar(filtro, pagina);
    }

    @GetMapping("/disponibilidad")
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Consultar disponibilidad (RF09)",
            description = "Para un rango inicio-fin devuelve cada espacio activo con libre/ocupado, la actividad que lo "
                    + "ocupa y el costo estimado (horas x precio). excluirTipo/excluirReferenciaId: para no chocar consigo "
                    + "mismo al editar. Rango maximo 31 dias.")
    public List<DisponibilidadResponse> disponibilidad(@ParameterObject DisponibilidadFiltro filtro) {
        return service.disponibilidad(filtro);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Ver espacio")
    public EspacioResponse obtener(@PathVariable @Positive Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Crear espacio", description = "Nombre unico (409).")
    public EspacioResponse crear(@Valid @RequestBody EspacioRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Editar espacio", description = "La capacidad no puede bajar de lo comprometido (cupo de ofertas o "
            + "asistentes de reservas/catering vigentes): 409. El precio nuevo solo aplica a reservas nuevas.")
    public EspacioResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody EspacioRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Desactivar espacio", description = "409 si tiene actividades futuras en la agenda.")
    public void desactivar(@PathVariable @Positive Long id) {
        service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Reactivar espacio")
    public void activar(@PathVariable @Positive Long id) {
        service.activar(id);
    }
}
