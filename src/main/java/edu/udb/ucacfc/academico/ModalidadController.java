package edu.udb.ucacfc.academico;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modalidades")
public class ModalidadController {

    private final ModalidadService modalidadService;

    public ModalidadController(ModalidadService modalidadService) {
        this.modalidadService = modalidadService;
    }

    @PostMapping
    public ResponseEntity<ModalidadResponseDTO> crear(@Valid @RequestBody ModalidadRequestDTO dto) {
        Modalidad creada = modalidadService.crear(
                Modalidad.builder().nombre(dto.nombre()).requiereEspacioFisico(dto.requiereEspacioFisico()).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ModalidadResponseDTO.desde(creada));
    }

    @GetMapping
    public List<ModalidadResponseDTO> listar() {
        return modalidadService.listar().stream().map(ModalidadResponseDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public ModalidadResponseDTO buscarPorId(@PathVariable Long id) {
        return ModalidadResponseDTO.desde(modalidadService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ModalidadResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody ModalidadRequestDTO dto) {
        Modalidad datos = Modalidad.builder().nombre(dto.nombre()).requiereEspacioFisico(dto.requiereEspacioFisico()).build();
        return ModalidadResponseDTO.desde(modalidadService.actualizar(id, datos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        modalidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
