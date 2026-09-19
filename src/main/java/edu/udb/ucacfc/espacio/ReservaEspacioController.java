package edu.udb.ucacfc.espacio;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas-espacio")
public class ReservaEspacioController {

    private final ReservaEspacioService reservaEspacioService;

    public ReservaEspacioController(ReservaEspacioService reservaEspacioService) {
        this.reservaEspacioService = reservaEspacioService;
    }

    // Este es EL endpoint para probar la regla de "no cruce de horarios":
    // llamalo dos veces con el mismo espacioId y horarios que se solapen,
    // la segunda vez debe responder 409 Conflict.
    @PostMapping
    public ResponseEntity<ReservaEspacioResponseDTO> reservar(@Valid @RequestBody ReservaEspacioRequestDTO dto) {
        ReservaEspacio creada = reservaEspacioService.reservar(
                dto.espacioId(), dto.clienteId(), dto.fechaHoraInicio(), dto.fechaHoraFin(), dto.motivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaEspacioResponseDTO.desde(creada));
    }

    @GetMapping("/paginado")
    public org.springframework.data.domain.Page<ReservaEspacioResponseDTO> listarPaginado(
            @org.springframework.data.web.PageableDefault(size = 10, sort = "id") org.springframework.data.domain.Pageable pageable) {
        return reservaEspacioService.listarPaginado(pageable).map(ReservaEspacioResponseDTO::desde);
    }

    @GetMapping
    public List<ReservaEspacioResponseDTO> listar(@RequestParam(required = false) Long clienteId) {
        List<ReservaEspacio> reservas = clienteId != null
                ? reservaEspacioService.listarPorCliente(clienteId)
                : reservaEspacioService.listar();
        return reservas.stream().map(ReservaEspacioResponseDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public ReservaEspacioResponseDTO buscarPorId(@PathVariable Long id) {
        return ReservaEspacioResponseDTO.desde(reservaEspacioService.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        reservaEspacioService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
