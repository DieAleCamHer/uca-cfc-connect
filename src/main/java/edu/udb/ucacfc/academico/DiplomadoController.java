package edu.udb.ucacfc.academico;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diplomados")
public class DiplomadoController {

    private final DiplomadoService diplomadoService;

    public DiplomadoController(DiplomadoService diplomadoService) {
        this.diplomadoService = diplomadoService;
    }

    @PostMapping
    public ResponseEntity<OfertaAcademicaResponseDTO> crear(@Valid @RequestBody OfertaAcademicaRequestDTO dto) {
        Diplomado creado = diplomadoService.crear(aEntidad(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(OfertaAcademicaResponseDTO.desde(creado));
    }

    @GetMapping
    public List<OfertaAcademicaResponseDTO> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        List<Diplomado> diplomados = soloActivos ? diplomadoService.listarActivos() : diplomadoService.listar();
        return diplomados.stream().map(OfertaAcademicaResponseDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public OfertaAcademicaResponseDTO buscarPorId(@PathVariable Long id) {
        return OfertaAcademicaResponseDTO.desde(diplomadoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public OfertaAcademicaResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody OfertaAcademicaRequestDTO dto) {
        return OfertaAcademicaResponseDTO.desde(diplomadoService.actualizar(id, aEntidad(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        diplomadoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    private Diplomado aEntidad(OfertaAcademicaRequestDTO dto) {
        Diplomado diplomado = new Diplomado();
        diplomado.setNombre(dto.nombre());
        diplomado.setCategoria(Categoria.builder().id(dto.categoriaId()).build());
        diplomado.setModalidad(Modalidad.builder().id(dto.modalidadId()).build());
        diplomado.setDocente(Docente.builder().id(dto.docenteId()).build());
        if (dto.espacioId() != null) {
            diplomado.setEspacio(edu.udb.ucacfc.espacio.Espacio.builder().id(dto.espacioId()).build());
        }
        diplomado.setCupoMaximo(dto.cupoMaximo());
        diplomado.setFechaInicio(dto.fechaInicio());
        diplomado.setFechaFin(dto.fechaFin());
        diplomado.setHoraInicio(dto.horaInicio());
        diplomado.setHoraFin(dto.horaFin());
        diplomado.setCosto(dto.costo());
        return diplomado;
    }
}
