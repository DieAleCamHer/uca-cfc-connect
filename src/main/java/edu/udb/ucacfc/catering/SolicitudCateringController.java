package edu.udb.ucacfc.catering;

import edu.udb.ucacfc.cliente.Cliente;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes-catering")
public class SolicitudCateringController {

    private final SolicitudCateringService solicitudCateringService;

    public SolicitudCateringController(SolicitudCateringService solicitudCateringService) {
        this.solicitudCateringService = solicitudCateringService;
    }

    @PostMapping
    public ResponseEntity<SolicitudCateringResponseDTO> solicitar(@Valid @RequestBody SolicitudCateringRequestDTO dto) {
        SolicitudCatering solicitud = new SolicitudCatering();
        solicitud.setCliente(Cliente.builder().id(dto.clienteId()).build());
        solicitud.setServicio(ServicioCatering.builder().id(dto.servicioId()).build());
        solicitud.setNumeroAsistentes(dto.numeroAsistentes());
        solicitud.setMenu(dto.menu());
        solicitud.setFecha(dto.fecha());
        solicitud.setHora(dto.hora());
        solicitud.setLugar(dto.lugar());
        SolicitudCatering creada = solicitudCateringService.solicitar(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(SolicitudCateringResponseDTO.desde(creada));
    }

    @GetMapping
    public List<SolicitudCateringResponseDTO> listar(@RequestParam(required = false) Long clienteId) {
        List<SolicitudCatering> solicitudes = clienteId != null
                ? solicitudCateringService.listarPorCliente(clienteId)
                : solicitudCateringService.listar();
        return solicitudes.stream().map(SolicitudCateringResponseDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public SolicitudCateringResponseDTO buscarPorId(@PathVariable Long id) {
        return SolicitudCateringResponseDTO.desde(solicitudCateringService.buscarPorId(id));
    }

    @PatchMapping("/{id}/estado")
    public SolicitudCateringResponseDTO cambiarEstado(@PathVariable Long id, @RequestParam EstadoSolicitudCatering estado) {
        return SolicitudCateringResponseDTO.desde(solicitudCateringService.cambiarEstado(id, estado));
    }
}
