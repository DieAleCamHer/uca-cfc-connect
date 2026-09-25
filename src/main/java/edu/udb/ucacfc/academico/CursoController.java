package edu.udb.ucacfc.academico;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @PostMapping
    public ResponseEntity<OfertaAcademicaResponseDTO> crear(@Valid @RequestBody OfertaAcademicaRequestDTO dto) {
        Curso creado = cursoService.crear(aEntidad(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(OfertaAcademicaResponseDTO.desde(creado));
    }

    @GetMapping
    public List<OfertaAcademicaResponseDTO> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        List<Curso> cursos = soloActivos ? cursoService.listarActivos() : cursoService.listar();
        return cursos.stream().map(OfertaAcademicaResponseDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public OfertaAcademicaResponseDTO buscarPorId(@PathVariable Long id) {
        return OfertaAcademicaResponseDTO.desde(cursoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public OfertaAcademicaResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody OfertaAcademicaRequestDTO dto) {
        return OfertaAcademicaResponseDTO.desde(cursoService.actualizar(id, aEntidad(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        cursoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    private Curso aEntidad(OfertaAcademicaRequestDTO dto) {
        Curso curso = new Curso();
        curso.setNombre(dto.nombre());
        curso.setCategoria(Categoria.builder().id(dto.categoriaId()).build());
        curso.setModalidad(Modalidad.builder().id(dto.modalidadId()).build());
        curso.setDocente(Docente.builder().id(dto.docenteId()).build());
        if (dto.espacioId() != null) {
            curso.setEspacio(edu.udb.ucacfc.espacio.Espacio.builder().id(dto.espacioId()).build());
        }
        curso.setCupoMaximo(dto.cupoMaximo());
        curso.setFechaInicio(dto.fechaInicio());
        curso.setFechaFin(dto.fechaFin());
        curso.setHoraInicio(dto.horaInicio());
        curso.setHoraFin(dto.horaFin());
        curso.setCosto(dto.costo());
        return curso;
    }
}
