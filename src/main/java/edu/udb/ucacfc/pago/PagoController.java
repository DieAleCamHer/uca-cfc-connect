package edu.udb.ucacfc.pago;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<PagoResponseDTO> crear(@Valid @RequestBody PagoRequestDTO dto) {
        Pago creado = pagoService.crear(dto.tipoReferencia(), dto.referenciaId(), dto.monto(), dto.metodo());
        return ResponseEntity.status(HttpStatus.CREATED).body(PagoResponseDTO.desde(creado));
    }

    @PatchMapping("/{id}/abonar")
    public PagoResponseDTO abonar(@PathVariable Long id, @Valid @RequestBody AbonoRequestDTO dto) {
        return PagoResponseDTO.desde(pagoService.abonar(id, dto.montoAbonado()));
    }

    @GetMapping("/{id}")
    public PagoResponseDTO buscarPorId(@PathVariable Long id) {
        return PagoResponseDTO.desde(pagoService.buscarPorId(id));
    }

    @GetMapping("/referencia")
    public List<PagoResponseDTO> listarPorReferencia(@RequestParam TipoReferenciaPago tipo, @RequestParam Long referenciaId) {
        return pagoService.listarPorReferencia(tipo, referenciaId).stream().map(PagoResponseDTO::desde).toList();
    }

    @GetMapping
    public List<PagoResponseDTO> listarPorEstado(@RequestParam EstadoPago estado) {
        return pagoService.listarPorEstado(estado).stream().map(PagoResponseDTO::desde).toList();
    }
}
