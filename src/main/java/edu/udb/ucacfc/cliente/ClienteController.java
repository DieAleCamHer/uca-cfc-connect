package edu.udb.ucacfc.cliente;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> crear(@Valid @RequestBody ClienteRequestDTO dto) {
        Cliente creado = clienteService.crear(aEntidad(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponseDTO.desde(creado));
    }

    @GetMapping
    public List<ClienteResponseDTO> listar(@RequestParam(required = false) String nombre) {
        List<Cliente> clientes = (nombre == null || nombre.isBlank())
                ? clienteService.listar()
                : clienteService.buscarPorNombre(nombre);
        return clientes.stream().map(ClienteResponseDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public ClienteResponseDTO buscarPorId(@PathVariable Long id) {
        return ClienteResponseDTO.desde(clienteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ClienteResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequestDTO dto) {
        return ClienteResponseDTO.desde(clienteService.actualizar(id, aEntidad(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private Cliente aEntidad(ClienteRequestDTO dto) {
        return Cliente.builder()
                .duiNit(dto.duiNit())
                .nombre(dto.nombre())
                .empresa(dto.empresa())
                .correo(dto.correo())
                .telefono(dto.telefono())
                .direccion(dto.direccion())
                .build();
    }
}
