package edu.udb.ucacfc.cotizacion;

import edu.udb.ucacfc.cliente.Cliente;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @PostMapping
    public ResponseEntity<CotizacionResponseDTO> crear(@Valid @RequestBody CotizacionRequestDTO dto) {
        Cotizacion cotizacion = Cotizacion.builder()
                .cliente(Cliente.builder().id(dto.clienteId()).build())
                .tipo(dto.tipo())
                .descripcion(dto.descripcion())
                .total(dto.total())
                .build();
        Cotizacion creada = cotizacionService.crear(cotizacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(CotizacionResponseDTO.desde(creada));
    }

    @GetMapping
    public List<CotizacionResponseDTO> listar(@RequestParam(required = false) Long clienteId) {
        List<Cotizacion> cotizaciones = clienteId != null
                ? cotizacionService.listarPorCliente(clienteId)
                : cotizacionService.listar();
        return cotizaciones.stream().map(CotizacionResponseDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public CotizacionResponseDTO buscarPorId(@PathVariable Long id) {
        return CotizacionResponseDTO.desde(cotizacionService.buscarPorId(id));
    }

    @PatchMapping("/{id}/estado")
    public CotizacionResponseDTO cambiarEstado(@PathVariable Long id, @RequestParam EstadoCotizacion estado) {
        return CotizacionResponseDTO.desde(cotizacionService.cambiarEstado(id, estado));
    }
}
