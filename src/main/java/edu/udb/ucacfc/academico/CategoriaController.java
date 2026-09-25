package edu.udb.ucacfc.academico;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> crear(@Valid @RequestBody CategoriaRequestDTO dto) {
        Categoria creada = categoriaService.crear(Categoria.builder().nombre(dto.nombre()).descripcion(dto.descripcion()).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoriaResponseDTO.desde(creada));
    }

    @GetMapping
    public List<CategoriaResponseDTO> listar() {
        return categoriaService.listar().stream().map(CategoriaResponseDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public CategoriaResponseDTO buscarPorId(@PathVariable Long id) {
        return CategoriaResponseDTO.desde(categoriaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public CategoriaResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequestDTO dto) {
        Categoria datos = Categoria.builder().nombre(dto.nombre()).descripcion(dto.descripcion()).build();
        return CategoriaResponseDTO.desde(categoriaService.actualizar(id, datos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
