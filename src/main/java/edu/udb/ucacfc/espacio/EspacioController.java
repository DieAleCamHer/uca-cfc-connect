package edu.udb.ucacfc.espacio;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    @PostMapping
    public ResponseEntity<EspacioResponseDTO> crear(@Valid @RequestBody EspacioRequestDTO dto) {
        Espacio creado = espacioService.crear(aEntidad(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(EspacioResponseDTO.desde(creado));
    }

    @GetMapping
    public List<EspacioResponseDTO> listar(@RequestParam(defaultValue = "false") boolean soloDisponibles) {
        List<Espacio> espacios = soloDisponibles ? espacioService.listarDisponibles() : espacioService.listar();
        return espacios.stream().map(EspacioResponseDTO::desde).toList();
    }

    @GetMapping("/paginado")
    public Page<EspacioResponseDTO> listarPaginado(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return espacioService.listarPaginado(pageable).map(EspacioResponseDTO::desde);
    }

    @GetMapping("/{id}")
    public EspacioResponseDTO buscarPorId(@PathVariable Long id) {
        return EspacioResponseDTO.desde(espacioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public EspacioResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody EspacioRequestDTO dto) {
        return EspacioResponseDTO.desde(espacioService.actualizar(id, aEntidad(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        espacioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private Espacio aEntidad(EspacioRequestDTO dto) {
        return Espacio.builder().nombre(dto.nombre()).tipo(dto.tipo()).capacidad(dto.capacidad())
                .precio(dto.precio()).equipamiento(dto.equipamiento()).disponible(true).build();
    }
}
