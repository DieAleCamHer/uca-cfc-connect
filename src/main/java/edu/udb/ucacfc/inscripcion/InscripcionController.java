package edu.udb.ucacfc.inscripcion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    public InscripcionController(InscripcionService inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    @PostMapping
    public ResponseEntity<InscripcionResponseDTO> inscribir(@RequestBody InscripcionRequestDTO dto) {
        Inscripcion creada = inscripcionService.inscribir(dto.clienteId(), dto.cursoId(), dto.diplomadoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(InscripcionResponseDTO.desde(creada));
    }

    @GetMapping
    public List<InscripcionResponseDTO> listar(@RequestParam(required = false) Long clienteId) {
        List<Inscripcion> inscripciones = clienteId != null
                ? inscripcionService.listarPorCliente(clienteId)
                : inscripcionService.listar();
        return inscripciones.stream().map(InscripcionResponseDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public InscripcionResponseDTO buscarPorId(@PathVariable Long id) {
        return InscripcionResponseDTO.desde(inscripcionService.buscarPorId(id));
    }

    @PatchMapping("/{id}/estado")
    public InscripcionResponseDTO cambiarEstado(@PathVariable Long id, @RequestParam EstadoInscripcion estado) {
        return InscripcionResponseDTO.desde(inscripcionService.cambiarEstado(id, estado));
    }
}
