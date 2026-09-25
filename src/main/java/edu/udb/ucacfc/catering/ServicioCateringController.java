package edu.udb.ucacfc.catering;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios-catering")
public class ServicioCateringController {

    private final ServicioCateringService servicioCateringService;

    public ServicioCateringController(ServicioCateringService servicioCateringService) {
        this.servicioCateringService = servicioCateringService;
    }

    @PostMapping
    public ResponseEntity<ServicioCateringResponseDTO> crear(@Valid @RequestBody ServicioCateringRequestDTO dto) {
        ServicioCatering creado = servicioCateringService.crear(
                ServicioCatering.builder().tipo(dto.tipo()).nombre(dto.nombre()).precioUnitario(dto.precioUnitario()).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ServicioCateringResponseDTO.desde(creado));
    }

    @GetMapping
    public List<ServicioCateringResponseDTO> listar() {
        return servicioCateringService.listar().stream().map(ServicioCateringResponseDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public ServicioCateringResponseDTO buscarPorId(@PathVariable Long id) {
        return ServicioCateringResponseDTO.desde(servicioCateringService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ServicioCateringResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody ServicioCateringRequestDTO dto) {
        ServicioCatering datos = ServicioCatering.builder().tipo(dto.tipo()).nombre(dto.nombre()).precioUnitario(dto.precioUnitario()).build();
        return ServicioCateringResponseDTO.desde(servicioCateringService.actualizar(id, datos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicioCateringService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
