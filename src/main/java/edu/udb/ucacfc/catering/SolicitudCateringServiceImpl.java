package edu.udb.ucacfc.catering;

import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteRepository;
import edu.udb.ucacfc.shared.exception.OperacionInvalidaException;
import edu.udb.ucacfc.shared.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SolicitudCateringServiceImpl implements SolicitudCateringService {

    private final SolicitudCateringRepository solicitudCateringRepository;
    private final ServicioCateringRepository servicioCateringRepository;
    private final ClienteRepository clienteRepository;

    public SolicitudCateringServiceImpl(SolicitudCateringRepository solicitudCateringRepository,
                                         ServicioCateringRepository servicioCateringRepository,
                                         ClienteRepository clienteRepository) {
        this.solicitudCateringRepository = solicitudCateringRepository;
        this.servicioCateringRepository = servicioCateringRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional
    public SolicitudCatering solicitar(SolicitudCatering solicitud) {
        if (solicitud.getNumeroAsistentes() == null || solicitud.getNumeroAsistentes() <= 0) {
            throw new OperacionInvalidaException("El numero de asistentes debe ser mayor a cero");
        }
        Cliente cliente = clienteRepository.findById(solicitud.getCliente().getId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", solicitud.getCliente().getId()));
        ServicioCatering servicio = servicioCateringRepository.findById(solicitud.getServicio().getId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("ServicioCatering", solicitud.getServicio().getId()));

        solicitud.setCliente(cliente);
        solicitud.setServicio(servicio);
        solicitud.setEstado(EstadoSolicitudCatering.PENDIENTE);
        return solicitudCateringRepository.save(solicitud);
    }

    @Override
    public SolicitudCatering buscarPorId(Long id) {
        return solicitudCateringRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("SolicitudCatering", id));
    }

    @Override
    public List<SolicitudCatering> listar() {
        return solicitudCateringRepository.findAll();
    }

    @Override
    public List<SolicitudCatering> listarPorCliente(Long clienteId) {
        return solicitudCateringRepository.findByClienteId(clienteId);
    }

    @Override
    @Transactional
    public SolicitudCatering cambiarEstado(Long id, EstadoSolicitudCatering nuevoEstado) {
        SolicitudCatering existente = buscarPorId(id);
        existente.setEstado(nuevoEstado);
        return solicitudCateringRepository.save(existente);
    }
}
