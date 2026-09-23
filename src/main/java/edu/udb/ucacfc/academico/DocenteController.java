package edu.udb.ucacfc.academico;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docentes")
public class DocenteController {

    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @PostMapping
    public ResponseEntity<DocenteResponseDTO> crear(@Valid @RequestBody DocenteRequestDTO dto) {
        Docente creado = docenteService.crear(aEntidad(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(DocenteResponseDTO.desde(creado));
    }

    @GetMapping
    public List<DocenteResponseDTO> listar() {
        return docenteService.listar().stream().map(DocenteResponseDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public DocenteResponseDTO buscarPorId(@PathVariable Long id) {
        return DocenteResponseDTO.desde(docenteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public DocenteResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody DocenteRequestDTO dto) {
        return DocenteResponseDTO.desde(docenteService.actualizar(id, aEntidad(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        docenteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private Docente aEntidad(DocenteRequestDTO dto) {
        return Docente.builder().nombre(dto.nombre()).especialidad(dto.especialidad())
                .correo(dto.correo()).telefono(dto.telefono()).build();
    }
}
