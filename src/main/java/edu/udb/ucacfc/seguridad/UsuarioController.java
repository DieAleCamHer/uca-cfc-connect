package edu.udb.ucacfc.seguridad;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody UsuarioRequestDTO dto) {
        Usuario usuario = Usuario.builder()
                .nombre(dto.nombre()).email(dto.email()).password(dto.password()).rol(dto.rol()).build();
        Usuario creado = usuarioService.registrar(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponseDTO.desde(creado));
    }

    @GetMapping
    public List<UsuarioResponseDTO> listar() {
        return usuarioService.listar().stream().map(UsuarioResponseDTO::desde).toList();
    }

    @GetMapping("/paginado")
    public Page<UsuarioResponseDTO> listarPaginado(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return usuarioService.listarPaginado(pageable).map(UsuarioResponseDTO::desde);
    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO buscarPorId(@PathVariable Long id) {
        return UsuarioResponseDTO.desde(usuarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public UsuarioResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequestDTO dto) {
        Usuario datos = Usuario.builder().nombre(dto.nombre()).rol(dto.rol()).build();
        return UsuarioResponseDTO.desde(usuarioService.actualizar(id, datos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        usuarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
