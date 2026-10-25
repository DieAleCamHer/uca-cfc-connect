package edu.udb.ucacfc.publico;

import edu.udb.ucacfc.academico.categoria.CategoriaResponse;
import edu.udb.ucacfc.academico.categoria.CategoriaService;
import edu.udb.ucacfc.academico.modalidad.ModalidadResponse;
import edu.udb.ucacfc.academico.modalidad.ModalidadService;
import edu.udb.ucacfc.academico.oferta.CursoService;
import edu.udb.ucacfc.academico.oferta.DiplomadoService;
import edu.udb.ucacfc.academico.oferta.dto.OfertaFiltro;
import edu.udb.ucacfc.academico.oferta.dto.OfertaResponse;
import edu.udb.ucacfc.catering.ServicioCateringService;
import edu.udb.ucacfc.catering.dto.ServicioCateringFiltro;
import edu.udb.ucacfc.catering.dto.ServicioCateringResponse;
import edu.udb.ucacfc.espacio.EspacioService;
import edu.udb.ucacfc.espacio.dto.EspacioFiltro;
import edu.udb.ucacfc.espacio.dto.EspacioResponse;
import edu.udb.ucacfc.shared.web.CatalogoFiltro;
import edu.udb.ucacfc.shared.web.PageResponse;
import edu.udb.ucacfc.shared.web.PaginaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lineamiento (Cliente): "Consultar la oferta academica". Publico y de solo lectura para la pagina de inicio:
 * solo muestra lo ACTIVO y vigente. Las rutas /api/publico/** tienen permitAll en SecurityConfig.
 */
@RestController
@RequestMapping("/api/publico")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "00. Publico", description = "Sin token: oferta academica vigente y catalogos para la pagina de inicio")
public class PublicoController {

    private final CursoService cursoService;
    private final DiplomadoService diplomadoService;
    private final CategoriaService categoriaService;
    private final ModalidadService modalidadService;
    private final EspacioService espacioService;
    private final ServicioCateringService servicioService;

    @GetMapping("/cursos")
    @Operation(summary = "Cursos vigentes", description = "Mismos filtros que /api/cursos; siempre activos y no finalizados.")
    public PageResponse<OfertaResponse> cursos(@ParameterObject OfertaFiltro filtro, @ParameterObject PaginaRequest pagina) {
        return cursoService.listarPublico(filtro, pagina);
    }

    @GetMapping("/diplomados")
    @Operation(summary = "Diplomados vigentes")
    public PageResponse<OfertaResponse> diplomados(@ParameterObject OfertaFiltro filtro, @ParameterObject PaginaRequest pagina) {
        return diplomadoService.listarPublico(filtro, pagina);
    }

    @GetMapping("/categorias")
    @Operation(summary = "Categorias activas")
    public PageResponse<CategoriaResponse> categorias() {
        return categoriaService.listar(new CatalogoFiltro(null, true), new PaginaRequest(0, 100, "nombre,asc"));
    }

    @GetMapping("/modalidades")
    @Operation(summary = "Modalidades activas")
    public PageResponse<ModalidadResponse> modalidades() {
        return modalidadService.listar(new CatalogoFiltro(null, true), new PaginaRequest(0, 100, "nombre,asc"));
    }

    @GetMapping("/espacios")
    @Operation(summary = "Espacios disponibles para alquiler")
    public PageResponse<EspacioResponse> espacios() {
        return espacioService.listar(new EspacioFiltro(null, null, true, null), new PaginaRequest(0, 100, "nombre,asc"));
    }

    @GetMapping("/servicios-catering")
    @Operation(summary = "Servicios de catering activos")
    public PageResponse<ServicioCateringResponse> servicios() {
        return servicioService.listar(new ServicioCateringFiltro(null, null, true), new PaginaRequest(0, 100, null));
    }
}
