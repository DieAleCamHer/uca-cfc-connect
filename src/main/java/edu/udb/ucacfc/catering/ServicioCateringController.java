package edu.udb.ucacfc.catering;

import edu.udb.ucacfc.catering.dto.ServicioCateringFiltro;
import edu.udb.ucacfc.catering.dto.ServicioCateringRequest;
import edu.udb.ucacfc.catering.dto.ServicioCateringResponse;
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
@RequestMapping("/api/servicios-catering")
@RequiredArgsConstructor
@Tag(name = "12. Catering - Servicios", description = "Catalogo (coffee break, desayuno, almuerzo, cena, refrigerio). Escritura: ADMIN")
public class ServicioCateringController {

    private final ServicioCateringService service;

    @GetMapping
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Listar servicios", description = "Filtros: q, tipo, activo. sort: id, tipo, nombre, precioUnitario, activo.")
    public PageResponse<ServicioCateringResponse> listar(@ParameterObject ServicioCateringFiltro filtro,
                                                         @ParameterObject PaginaRequest pagina) {
        return service.listar(filtro, pagina);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.AUTENTICADO)
    @Operation(summary = "Ver servicio")
    public ServicioCateringResponse obtener(@PathVariable @Positive Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Crear servicio", description = "Precio POR PERSONA. tipo + nombre unicos.")
    public ServicioCateringResponse crear(@Valid @RequestBody ServicioCateringRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Editar servicio", description = "El precio nuevo no cambia pedidos ya registrados (congelan el suyo).")
    public ServicioCateringResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody ServicioCateringRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Desactivar servicio", description = "409 si tiene pedidos vigentes a futuro.")
    public void desactivar(@PathVariable @Positive Long id) {
        service.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permisos.ADMIN)
    @Operation(summary = "Reactivar servicio")
    public void activar(@PathVariable @Positive Long id) {
        service.activar(id);
    }
}
